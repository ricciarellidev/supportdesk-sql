package dev.ricciarelli.supportdesk.web.model;

import dev.ricciarelli.supportdesk.domain.Priority;
import dev.ricciarelli.supportdesk.domain.TicketStatus;

import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;

public final class TicketModels {
    private TicketModels() {}

    public record CreateTicket(
            @NotBlank @Size(max = 140) String title,
            @NotBlank @Size(max = 4000) String description,
            @Positive long requesterId,
            @Positive long categoryId,
            @NotNull Priority priority) {}

    public record Assignment(@Positive long agentId, @Positive long actorId) {}

    public record Transition(
            @NotNull TicketStatus status,
            @Positive long actorId,
            @NotBlank @Size(max = 2000) String message) {}

    public record Comment(@Positive long actorId, @NotBlank @Size(max = 2000) String message) {}

    public record TicketView(
            long id,
            String title,
            String description,
            long requesterId,
            String requesterName,
            long categoryId,
            String categoryName,
            long departmentId,
            String departmentName,
            Long assignedAgentId,
            String assignedAgentName,
            Priority priority,
            TicketStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime dueAt,
            OffsetDateTime resolvedAt,
            OffsetDateTime closedAt) {}

    public record EventView(
            long id,
            long actorId,
            String actorName,
            String eventType,
            String previousStatus,
            String newStatus,
            String message,
            OffsetDateTime createdAt) {}

    public record TicketDetail(TicketView ticket, List<EventView> history) {}

    public record TicketPage(
            List<TicketView> content, long totalElements, int page, int size, long totalPages) {}

    public record TicketFilter(
            TicketStatus status,
            Priority priority,
            Long categoryId,
            Long agentId,
            String query,
            int page,
            int size) {}
}
