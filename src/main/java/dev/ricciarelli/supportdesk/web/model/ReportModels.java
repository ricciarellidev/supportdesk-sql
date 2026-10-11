package dev.ricciarelli.supportdesk.web.model;

import java.math.BigDecimal;

public final class ReportModels {
    private ReportModels() {}

    public record Overview(
            long totalTickets,
            long openBacklog,
            long resolvedTickets,
            long overdueBacklog,
            BigDecimal averageResolutionHours) {}

    public record CategoryReport(
            long categoryId,
            String categoryName,
            long totalTickets,
            long openBacklog,
            BigDecimal averageResolutionHours) {}

    public record AgentReport(
            long agentId,
            String agentName,
            long assignedTickets,
            long openBacklog,
            long resolvedTickets) {}

    public record SlaReport(
            String priority, long resolvedTickets, long withinSla, BigDecimal compliancePercent) {}
}
