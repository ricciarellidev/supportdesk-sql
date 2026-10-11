package dev.ricciarelli.supportdesk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;

class TicketApiIT extends PostgresTestSupport {
    @BeforeEach
    void fixture() {
        basicFixture();
    }

    @Test
    void createsPersistentTicketWithDeadlineAndHistory() throws Exception {
        var response =
                send(
                        "POST",
                        "/api/v1/tickets",
                        """
{"title":"  Falha no portal  ","description":"Não consigo acessar.","requesterId":1,"categoryId":1,"priority":"CRITICAL"}
""");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        Number id = JsonPath.read(response.body(), "$.ticket.id");
        assertThat(response.headers().firstValue("Location")).contains("/api/v1/tickets/" + id);
        assertThat(response.headers().firstValue("X-Request-Id")).contains("integration_test");
        assertThat(JsonPath.<String>read(response.body(), "$.ticket.title"))
                .isEqualTo("Falha no portal");
        OffsetDateTime created =
                OffsetDateTime.parse(JsonPath.read(response.body(), "$.ticket.createdAt"));
        OffsetDateTime due = OffsetDateTime.parse(JsonPath.read(response.body(), "$.ticket.dueAt"));
        assertThat(Duration.between(created, due)).isEqualTo(Duration.ofHours(4));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tickets", Long.class)).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(response.body(), "$.history.length()")).isEqualTo(1);
        assertThat(send("GET", "/api/v1/tickets/" + id, null).body()).isEqualTo(response.body());
    }

    @Test
    void supportsLifecycleAndPreservesAuditAfterArchive() throws Exception {
        long id = createTicket("Ciclo completo");
        assertThat(assign(id).statusCode()).isEqualTo(200);
        assertThat(transition(id, "IN_PROGRESS").statusCode()).isEqualTo(200);
        assertThat(transition(id, "RESOLVED").statusCode()).isEqualTo(200);
        var closed = transition(id, "CLOSED");
        assertThat(closed.statusCode()).as(closed.body()).isEqualTo(200);
        assertThat(JsonPath.<String>read(closed.body(), "$.ticket.closedAt")).isNotBlank();
        assertThat(send("DELETE", "/api/v1/tickets/" + id + "?actorId=2", null).statusCode())
                .isEqualTo(204);
        assertThat(send("GET", "/api/v1/tickets/" + id, null).statusCode()).isEqualTo(404);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM tickets WHERE archived_at IS NOT NULL",
                                Long.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM ticket_events WHERE ticket_id = ?",
                                Long.class,
                                id))
                .isEqualTo(6);
        assertThat(
                        JsonPath.<Integer>read(
                                send("GET", "/api/v1/tickets", null).body(), "$.totalElements"))
                .isZero();
        assertThat(
                        JsonPath.<Integer>read(
                                send("GET", "/api/v1/reports/overview", null).body(),
                                "$.totalTickets"))
                .isZero();
    }

    @Test
    void requiresAssigneeAndEnforcesStatusSequence() throws Exception {
        long id = createTicket("Validar sequência");
        assertThat(transition(id, "IN_PROGRESS").statusCode()).isEqualTo(409);
        assign(id);
        var invalid = transition(id, "CLOSED");
        assertThat(invalid.statusCode()).isEqualTo(409);
        assertThat(JsonPath.<String>read(invalid.body(), "$.code")).isEqualTo("INVALID_TRANSITION");
        assertThat(
                        JsonPath.<String>read(
                                send("GET", "/api/v1/tickets/" + id, null).body(),
                                "$.ticket.status"))
                .isEqualTo("OPEN");
    }

    @Test
    void reopensWithoutResettingTheOriginalSlaDeadline() throws Exception {
        long id = createTicket("Reabertura");
        assign(id);
        transition(id, "IN_PROGRESS");
        transition(id, "RESOLVED");
        String before = send("GET", "/api/v1/tickets/" + id, null).body();
        var reopened = transition(id, "IN_PROGRESS");
        assertThat(reopened.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Object>read(reopened.body(), "$.ticket.resolvedAt")).isNull();
        assertThat(JsonPath.<String>read(reopened.body(), "$.ticket.dueAt"))
                .isEqualTo(JsonPath.read(before, "$.ticket.dueAt"));
        transition(id, "RESOLVED");
        transition(id, "CLOSED");
        assertThat(transition(id, "IN_PROGRESS").statusCode()).isEqualTo(409);
    }

    @Test
    void makesRepeatedAssignmentIdempotent() throws Exception {
        long id = createTicket("Atribuição");
        assign(id);
        assign(id);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM ticket_events WHERE event_type = 'ASSIGNED'",
                                Long.class))
                .isEqualTo(1);
    }

    @Test
    void storesCommentWithoutChangingStatus() throws Exception {
        long id = createTicket("Comentários");
        var response =
                send(
                        "POST",
                        "/api/v1/tickets/" + id + "/comments",
                        "{\"actorId\":1,\"message\":\"Evidência adicionada.\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.ticket.status")).isEqualTo("OPEN");
        assertThat(JsonPath.<String>read(response.body(), "$.history[1].message"))
                .isEqualTo("Evidência adicionada.");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"agentId\":1,\"actorId\":2}",
                "{\"agentId\":3,\"actorId\":2}",
                "{\"agentId\":2,\"actorId\":1}"
            })
    void rejectsInvalidAgents(String body) throws Exception {
        long id = createTicket("Validação de agente");
        assertThat(send("PATCH", "/api/v1/tickets/" + id + "/assignment", body).statusCode())
                .isEqualTo(409);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM tickets WHERE assigned_agent_id IS NOT NULL",
                                Long.class))
                .isZero();
    }

    @Test
    void rejectsMissingReferencesAndInvalidRequesterRoles() throws Exception {
        assertThat(
                        send(
                                        "POST",
                                        "/api/v1/tickets",
                                        "{\"title\":\"Teste\",\"description\":\"Teste\",\"requesterId\":999,\"categoryId\":1,\"priority\":\"HIGH\"}")
                                .statusCode())
                .isEqualTo(404);
        assertThat(
                        send(
                                        "POST",
                                        "/api/v1/tickets",
                                        "{\"title\":\"Teste\",\"description\":\"Teste\",\"requesterId\":2,\"categoryId\":1,\"priority\":\"HIGH\"}")
                                .statusCode())
                .isEqualTo(409);
        assertThat(
                        send(
                                        "POST",
                                        "/api/v1/tickets",
                                        "{\"title\":\"Teste\",\"description\":\"Teste\",\"requesterId\":1,\"categoryId\":999,\"priority\":\"HIGH\"}")
                                .statusCode())
                .isEqualTo(404);
        assertThat(send("GET", "/api/v1/tickets/999", null).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tickets", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "?size=0",
                "?size=101",
                "?page=-1",
                "?page=10001",
                "?status=INVALID",
                "?priority=INVALID",
                "?categoryId=0",
                "?agentId=-2"
            })
    void validatesListParameters(String query) throws Exception {
        assertThat(send("GET", "/api/v1/tickets" + query, null).statusCode()).isEqualTo(400);
    }

    @Test
    void supportsSearchAndPaginationWithoutSqlInjection() throws Exception {
        createTicket("Acesso ao portal");
        createTicket("Falha no banco");
        createTicket("Relatório mensal");
        var first = send("GET", "/api/v1/tickets?size=2&page=0", null);
        assertThat(JsonPath.<Integer>read(first.body(), "$.totalElements")).isEqualTo(3);
        assertThat(JsonPath.<Integer>read(first.body(), "$.totalPages")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(first.body(), "$.content.length()")).isEqualTo(2);
        var filtered =
                send(
                        "GET",
                        "/api/v1/tickets?status=OPEN&priority=HIGH&categoryId=1&q=portal",
                        null);
        assertThat(JsonPath.<Integer>read(filtered.body(), "$.totalElements")).isEqualTo(1);
        for (String query : new String[] {"' OR 1=1 --", "%", "_", "\\"}) {
            var response =
                    send(
                            "GET",
                            "/api/v1/tickets?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8),
                            null);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            assertThat(JsonPath.<Integer>read(response.body(), "$.totalElements")).isZero();
        }
    }

    @Test
    void validatesBodyAndDoesNotLeakDatabaseDetails() throws Exception {
        var blank =
                send(
                        "POST",
                        "/api/v1/tickets",
                        "{\"title\":\" \",\"description\":\""
                            + " \",\"requesterId\":0,\"categoryId\":0,\"priority\":null}");
        assertThat(blank.statusCode()).isEqualTo(400);
        assertThat(blank.body())
                .contains("VALIDATION_ERROR", "errors")
                .doesNotContain("SELECT", "INSERT");
        assertThat(send("POST", "/api/v1/tickets", "{\"priority\":\"INVALID\"}").statusCode())
                .isEqualTo(400);
        assertThat(send("POST", "/api/v1/tickets", "broken-json").statusCode()).isEqualTo(400);
    }

    @Test
    void refusesArchivingAnOpenTicket() throws Exception {
        long id = createTicket("Não arquivar aberto");
        assertThat(send("DELETE", "/api/v1/tickets/" + id + "?actorId=2", null).statusCode())
                .isEqualTo(409);
        assertThat(send("DELETE", "/api/v1/tickets/" + id, null).statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsFractionalIdentifiersInsteadOfTruncatingThem() throws Exception {
        var response =
                send(
                        "POST",
                        "/api/v1/tickets",
                        """
{"title":"Teste","description":"Identificador inválido.","requesterId":1.5,"categoryId":1,"priority":"HIGH"}
""");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tickets", Long.class)).isZero();
    }

    @Test
    void preservesFrameworkHttpStatusAndAllowHeader() throws Exception {
        var response = send("PUT", "/api/v1/tickets", "{}");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(405);
        assertThat(response.headers().firstValue("Allow").orElseThrow()).contains("GET", "POST");
        assertThat(send("GET", "/api/v1/not-a-resource", null).statusCode()).isEqualTo(404);
    }

    @Test
    void rollsBackStatusWhenAuditPersistenceFails() throws Exception {
        long id = createTicket("Rollback");
        assign(id);
        jdbc.execute(
                """
CREATE FUNCTION reject_status_event() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.event_type = 'STATUS_CHANGED' THEN RAISE EXCEPTION 'simulated_storage_failure'; END IF;
    RETURN NEW;
END;
$$
""");
        jdbc.execute(
                "CREATE TRIGGER reject_status_event BEFORE INSERT ON ticket_events FOR EACH ROW"
                    + " EXECUTE FUNCTION reject_status_event()");
        var failed = transition(id, "IN_PROGRESS");
        assertThat(failed.statusCode()).as(failed.body()).isEqualTo(500);
        assertThat(failed.body()).doesNotContain("simulated_storage_failure", "UPDATE tickets");
        assertThat(jdbc.queryForObject("SELECT status FROM tickets WHERE id = ?", String.class, id))
                .isEqualTo("OPEN");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM ticket_events WHERE ticket_id = ?",
                                Long.class,
                                id))
                .isEqualTo(2);
    }

    @Test
    void replacesInvalidRequestIdAndExposesHealthAndDashboard() throws Exception {
        var response = send("GET", "/api/v1/tickets", null, "invalid/request/id");
        assertThat(response.headers().firstValue("X-Request-Id").orElseThrow())
                .matches("[a-f0-9-]{36}");
        assertThat(send("GET", "/actuator/health", null).statusCode()).isEqualTo(200);
        var dashboard = send("GET", "/", null);
        assertThat(dashboard.statusCode()).isEqualTo(200);
        assertThat(dashboard.body()).contains("SupportDesk SQL");
    }
}
