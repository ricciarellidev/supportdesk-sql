package dev.ricciarelli.supportdesk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogApiIT extends PostgresTestSupport {
    @BeforeEach
    void fixture() {
        basicFixture();
    }

    @Test
    void createsAndListsCatalogsWithNormalizedEmail() throws Exception {
        var department = send("POST", "/api/v1/departments", "{\"name\":\"Financeiro\"}");
        assertThat(department.statusCode()).isEqualTo(201);
        Number departmentId = JsonPath.read(department.body(), "$.id");
        var category =
                send(
                        "POST",
                        "/api/v1/categories",
                        "{\"name\":\"Relatórios\",\"departmentId\":" + departmentId + "}");
        assertThat(category.statusCode()).as(category.body()).isEqualTo(201);
        var user =
                send(
                        "POST",
                        "/api/v1/users",
                        "{\"name\":\"Pessoa"
                            + " fictícia\",\"email\":\"TEST@EXAMPLE.COM\",\"role\":\"REQUESTER\",\"departmentId\":"
                                + departmentId
                                + "}");
        assertThat(user.statusCode()).as(user.body()).isEqualTo(201);
        assertThat(JsonPath.<String>read(user.body(), "$.email")).isEqualTo("test@example.com");
        assertThat(send("GET", "/api/v1/departments", null).body()).contains("Financeiro");
        assertThat(send("GET", "/api/v1/categories", null).body()).contains("Relatórios");
        assertThat(send("GET", "/api/v1/users", null).body()).contains("test@example.com");
    }

    @Test
    void rejectsDuplicatesWithASanitizedConflictResponse() throws Exception {
        var duplicate =
                send(
                        "POST",
                        "/api/v1/users",
                        "{\"name\":\"Duplicado\",\"email\":\"AGENT@EXAMPLE.COM\",\"role\":\"AGENT\",\"departmentId\":1}");
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");
        assertThat(duplicate.body())
                .contains("DATA_CONFLICT")
                .doesNotContain("app_users_email_key", "INSERT", "SQLException");
    }

    @Test
    void rejectsInvalidCatalogFieldsAndUnknownDepartments() throws Exception {
        assertThat(
                        send(
                                        "POST",
                                        "/api/v1/departments",
                                        """
                                        {"name":" "}
                                        """)
                                .statusCode())
                .isEqualTo(400);
        assertThat(
                        send(
                                        "POST",
                                        "/api/v1/users",
                                        "{\"name\":\"Teste\",\"email\":\"invalid\",\"role\":\"AGENT\",\"departmentId\":1}")
                                .statusCode())
                .isEqualTo(400);
        assertThat(
                        send(
                                        "POST",
                                        "/api/v1/categories",
                                        "{\"name\":\"Teste\",\"departmentId\":999}")
                                .statusCode())
                .isEqualTo(404);
    }
}
