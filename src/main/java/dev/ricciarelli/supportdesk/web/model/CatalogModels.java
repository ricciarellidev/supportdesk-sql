package dev.ricciarelli.supportdesk.web.model;

import dev.ricciarelli.supportdesk.domain.UserRole;

import jakarta.validation.constraints.*;

public final class CatalogModels {
    private CatalogModels() {}

    public record Department(long id, String name) {}

    public record Category(long id, String name, long departmentId, String departmentName) {}

    public record User(
            long id, String name, String email, UserRole role, long departmentId, boolean active) {}

    public record CreateDepartment(@NotBlank @Size(max = 80) String name) {}

    public record CreateCategory(
            @NotBlank @Size(max = 80) String name, @Positive long departmentId) {}

    public record CreateUser(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @NotNull UserRole role,
            @Positive long departmentId) {}
}
