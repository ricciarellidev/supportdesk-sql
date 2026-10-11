package dev.ricciarelli.supportdesk.persistence;

import dev.ricciarelli.supportdesk.domain.UserRole;
import dev.ricciarelli.supportdesk.web.model.CatalogModels.*;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CatalogRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public CatalogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Department> departments() {
        return jdbc.query(
                "SELECT id, name FROM departments ORDER BY name, id",
                Map.of(),
                (rs, row) -> new Department(rs.getLong("id"), rs.getString("name")));
    }

    public boolean departmentExists(long id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM departments WHERE id = :id)",
                        Map.of("id", id),
                        Boolean.class));
    }

    public Department createDepartment(String name) {
        return jdbc.queryForObject(
                "INSERT INTO departments (name) VALUES (:name) RETURNING id, name",
                Map.of("name", name),
                (rs, row) -> new Department(rs.getLong("id"), rs.getString("name")));
    }

    public List<Category> categories() {
        return jdbc.query(
                """
SELECT c.id, c.name, c.department_id, d.name AS department_name
FROM categories c JOIN departments d ON d.id = c.department_id ORDER BY c.name, c.id
""",
                Map.of(),
                (rs, row) ->
                        new Category(
                                rs.getLong("id"),
                                rs.getString("name"),
                                rs.getLong("department_id"),
                                rs.getString("department_name")));
    }

    public boolean categoryExists(long id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM categories WHERE id = :id)",
                        Map.of("id", id),
                        Boolean.class));
    }

    public Category createCategory(String name, long departmentId) {
        Long id =
                jdbc.queryForObject(
                        """
INSERT INTO categories (name, department_id) VALUES (:name, :departmentId) RETURNING id
""",
                        Map.of("name", name, "departmentId", departmentId),
                        Long.class);
        return jdbc.queryForObject(
                """
                SELECT c.id, c.name, c.department_id, d.name AS department_name
                FROM categories c JOIN departments d ON d.id = c.department_id WHERE c.id = :id
                """,
                Map.of("id", id),
                (rs, row) ->
                        new Category(
                                rs.getLong("id"),
                                rs.getString("name"),
                                rs.getLong("department_id"),
                                rs.getString("department_name")));
    }

    public List<User> users() {
        return jdbc.query(
                "SELECT id, name, email, role, department_id, active FROM app_users ORDER BY name,"
                    + " id",
                Map.of(),
                (rs, row) ->
                        new User(
                                rs.getLong("id"),
                                rs.getString("name"),
                                rs.getString("email"),
                                UserRole.valueOf(rs.getString("role")),
                                rs.getLong("department_id"),
                                rs.getBoolean("active")));
    }

    public Optional<User> findUser(long id) {
        return jdbc
                .query(
                        "SELECT id, name, email, role, department_id, active FROM app_users WHERE"
                            + " id = :id",
                        Map.of("id", id),
                        (rs, row) ->
                                new User(
                                        rs.getLong("id"),
                                        rs.getString("name"),
                                        rs.getString("email"),
                                        UserRole.valueOf(rs.getString("role")),
                                        rs.getLong("department_id"),
                                        rs.getBoolean("active")))
                .stream()
                .findFirst();
    }

    public User createUser(CreateUser request) {
        Long id =
                jdbc.queryForObject(
                        """
                        INSERT INTO app_users (name, email, role, department_id)
                        VALUES (:name, :email, :role, :departmentId) RETURNING id
                        """,
                        Map.of(
                                "name",
                                request.name().trim(),
                                "email",
                                request.email().trim().toLowerCase(java.util.Locale.ROOT),
                                "role",
                                request.role().name(),
                                "departmentId",
                                request.departmentId()),
                        Long.class);
        return findUser(id).orElseThrow();
    }
}
