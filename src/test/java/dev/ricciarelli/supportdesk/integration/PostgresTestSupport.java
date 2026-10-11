package dev.ricciarelli.supportdesk.integration;

import com.jayway.jsonpath.JsonPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class PostgresTestSupport {
    @Value("${local.server.port}")
    private int port;

    @Autowired protected JdbcTemplate jdbc;
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url =
                System.getenv()
                        .getOrDefault(
                                "TEST_DATABASE_URL",
                                "jdbc:postgresql://localhost:5433/supportdesk_test");
        // The suite truncates its own fixture tables. Refuse application or arbitrary database
        // names.
        if (!url.startsWith("jdbc:postgresql://")
                || !"/supportdesk_test".equals(URI.create(url.substring(5)).getPath())) {
            throw new IllegalArgumentException(
                    "Integration tests require a database named supportdesk_test.");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add(
                "spring.datasource.username",
                () -> System.getenv().getOrDefault("TEST_DATABASE_USER", "supportdesk"));
        properties.add(
                "spring.datasource.password",
                () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "local_demo_only"));
        properties.add(
                "spring.datasource.hikari.maximum-pool-size",
                () ->
                        Integer.parseInt(
                                System.getenv().getOrDefault("TEST_DATABASE_POOL_SIZE", "2")));
        properties.add("spring.flyway.locations", () -> "classpath:db/migration");
        properties.add("spring.profiles.active", () -> "test");
    }

    protected void reset() {
        jdbc.execute("DROP TRIGGER IF EXISTS reject_status_event ON ticket_events");
        jdbc.execute("DROP FUNCTION IF EXISTS reject_status_event()");
        jdbc.execute(
                "TRUNCATE ticket_events, tickets, categories, app_users, departments RESTART"
                    + " IDENTITY CASCADE");
    }

    protected void basicFixture() {
        reset();
        jdbc.execute(
                "INSERT INTO departments (id, name) VALUES (1, 'Tecnologia'), (2, 'Operações')");
        jdbc.execute(
                """
                INSERT INTO app_users (id, name, email, role, department_id, active) VALUES
                (1, 'Solicitante', 'requester@example.com', 'REQUESTER', 2, TRUE),
                (2, 'Agente', 'agent@example.com', 'AGENT', 1, TRUE),
                (3, 'Agente inativo', 'inactive@example.com', 'AGENT', 1, FALSE)
                """);
        jdbc.execute(
                "INSERT INTO categories (id, name, department_id) VALUES (1, 'Acesso', 1), (2,"
                    + " 'Banco de dados', 1)");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('departments','id'), 2)");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('app_users','id'), 3)");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('categories','id'), 2)");
    }

    protected HttpResponse<String> send(String method, String path, String body) throws Exception {
        return send(method, path, body, "integration_test");
    }

    protected HttpResponse<String> send(String method, String path, String body, String requestId)
            throws Exception {
        var builder =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json");
        if (requestId != null) builder.header("X-Request-Id", requestId);
        builder.method(
                method,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected long createTicket(String title) throws Exception {
        var response =
                send(
                        "POST",
                        "/api/v1/tickets",
                        """
{"title":"%s","description":"Descrição do chamado de teste.","requesterId":1,"categoryId":1,"priority":"HIGH"}
"""
                                .formatted(title));
        org.assertj.core.api.Assertions.assertThat(response.statusCode())
                .as(response.body())
                .isEqualTo(201);
        return ((Number) JsonPath.read(response.body(), "$.ticket.id")).longValue();
    }

    protected HttpResponse<String> assign(long id) throws Exception {
        return send(
                "PATCH", "/api/v1/tickets/" + id + "/assignment", "{\"agentId\":2,\"actorId\":2}");
    }

    protected HttpResponse<String> transition(long id, String status) throws Exception {
        return send(
                "PATCH",
                "/api/v1/tickets/" + id + "/status",
                "{\"status\":\""
                        + status
                        + "\",\"actorId\":2,\"message\":\"Alteração de teste.\"}");
    }
}
