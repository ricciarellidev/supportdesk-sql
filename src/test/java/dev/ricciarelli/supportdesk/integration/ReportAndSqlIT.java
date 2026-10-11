package dev.ricciarelli.supportdesk.integration;

import static org.assertj.core.api.Assertions.*;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class ReportAndSqlIT extends PostgresTestSupport {
    @BeforeEach
    void fixture() throws Exception {
        reset();
        try (var input =
                new ClassPathResource("db/demo/V900__load_fictional_demo.sql").getInputStream()) {
            jdbc.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void calculatesOverviewFromTheDocumentedDataset() throws Exception {
        var response = send("GET", "/api/v1/reports/overview", null);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.totalTickets")).isEqualTo(8);
        assertThat(JsonPath.<Integer>read(response.body(), "$.openBacklog")).isEqualTo(4);
        assertThat(JsonPath.<Integer>read(response.body(), "$.resolvedTickets")).isEqualTo(4);
        assertThat(
                        ((Number) JsonPath.read(response.body(), "$.averageResolutionHours"))
                                .doubleValue())
                .isEqualTo(5.0);
    }

    @Test
    void includesCategoriesWithZeroTicketsWithoutCountingTheLeftJoinRow() throws Exception {
        jdbc.update("INSERT INTO categories (name, department_id) VALUES ('Sem chamados', 1)");
        var response = send("GET", "/api/v1/reports/by-category", null);
        List<Map<String, Object>> rows = JsonPath.read(response.body(), "$");
        Map<String, Object> zero =
                rows.stream()
                        .filter(row -> row.get("categoryName").equals("Sem chamados"))
                        .findFirst()
                        .orElseThrow();
        assertThat(zero.get("totalTickets")).isEqualTo(0);
        assertThat(zero.get("openBacklog")).isEqualTo(0);
        assertThat(zero.get("averageResolutionHours")).isNull();
    }

    @Test
    void includesAnUnassignedActiveAgentButExcludesInactiveUsers() throws Exception {
        var response = send("GET", "/api/v1/reports/by-agent", null);
        List<Map<String, Object>> rows = JsonPath.read(response.body(), "$");
        assertThat(rows).hasSize(3);
        Map<String, Object> bianca =
                rows.stream()
                        .filter(row -> row.get("agentName").equals("Bianca Alves"))
                        .findFirst()
                        .orElseThrow();
        assertThat(bianca.get("assignedTickets")).isEqualTo(0);
        assertThat(response.body()).doesNotContain("Conta Inativa");
    }

    @Test
    void measuresSlaOnlyAmongResolvedTicketsAndRepresentsNoDenominatorAsNull() throws Exception {
        var response = send("GET", "/api/v1/reports/sla", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(
                        ((Number) JsonPath.read(response.body(), "$[1].compliancePercent"))
                                .doubleValue())
                .isEqualTo(50.0);
        assertThat(JsonPath.<Integer>read(response.body(), "$[1].resolvedTickets")).isEqualTo(2);
        assertThat(JsonPath.<Object>read(response.body(), "$[3].compliancePercent")).isNull();
        assertThat(JsonPath.<Integer>read(response.body(), "$[3].resolvedTickets")).isZero();
    }

    @Test
    void executesEveryReadOnlySqlWorkbookExample() throws Exception {
        Map<String, Integer> expected =
                Map.of(
                        "01_backlog_join.sql",
                        4,
                        "02_category_group_by.sql",
                        4,
                        "03_agents_left_join.sql",
                        3,
                        "04_sla_case.sql",
                        3,
                        "05_daily_volume_cte.sql",
                        1,
                        "06_rank_agents_window.sql",
                        3,
                        "07_categories_having.sql",
                        3,
                        "08_requesters_exists.sql",
                        2,
                        "09_latest_ticket_subquery.sql",
                        2,
                        "12_department_workload.sql",
                        3);
        for (var entry : expected.entrySet()) {
            var result = jdbc.queryForList(Files.readString(Path.of("sql", entry.getKey())));
            assertThat(result).as(entry.getKey()).hasSize(entry.getValue());
        }
    }

    @Test
    void rollsBackTheCrudWorkbookAndExecutesTheQueryPlan() throws Exception {
        jdbc.execute(Files.readString(Path.of("sql/10_crud_transaction.sql")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Long.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM departments", Long.class))
                .isEqualTo(3);
        assertThat(jdbc.queryForList(Files.readString(Path.of("sql/11_explain_backlog.sql"))))
                .isNotEmpty();
    }

    @Test
    void enforcesForeignKeysUniquenessAndLifecycleChecksAtDatabaseLevel() {
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO categories (name, department_id) VALUES"
                                            + " ('Inválida', 999)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () -> jdbc.update("INSERT INTO departments (name) VALUES ('Tecnologia')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE tickets SET status = 'CLOSED' WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE tickets SET due_at = created_at WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM tickets WHERE id = 1", String.class))
                .isEqualTo("OPEN");
    }
}
