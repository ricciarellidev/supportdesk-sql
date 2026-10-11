package dev.ricciarelli.supportdesk.persistence;

import dev.ricciarelli.supportdesk.web.model.ReportModels.*;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Repository
public class ReportRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String overview = sql("overview");
    private final String byCategory = sql("by-category");
    private final String byAgent = sql("by-agent");
    private final String sla = sql("sla");

    public ReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Overview overview() {
        return jdbc.queryForObject(
                overview,
                Map.of(),
                (rs, row) ->
                        new Overview(
                                rs.getLong("total_tickets"),
                                rs.getLong("open_backlog"),
                                rs.getLong("resolved_tickets"),
                                rs.getLong("overdue_backlog"),
                                rs.getBigDecimal("average_resolution_hours")));
    }

    public List<CategoryReport> byCategory() {
        return jdbc.query(
                byCategory,
                Map.of(),
                (rs, row) ->
                        new CategoryReport(
                                rs.getLong("category_id"),
                                rs.getString("category_name"),
                                rs.getLong("total_tickets"),
                                rs.getLong("open_backlog"),
                                rs.getBigDecimal("average_resolution_hours")));
    }

    public List<AgentReport> byAgent() {
        return jdbc.query(
                byAgent,
                Map.of(),
                (rs, row) ->
                        new AgentReport(
                                rs.getLong("agent_id"),
                                rs.getString("agent_name"),
                                rs.getLong("assigned_tickets"),
                                rs.getLong("open_backlog"),
                                rs.getLong("resolved_tickets")));
    }

    public List<SlaReport> sla() {
        return jdbc.query(
                sla,
                Map.of(),
                (rs, row) ->
                        new SlaReport(
                                rs.getString("priority"),
                                rs.getLong("resolved_tickets"),
                                rs.getLong("within_sla"),
                                rs.getBigDecimal("compliance_percent")));
    }

    private static String sql(String name) {
        try (var input = new ClassPathResource("sql/reports/" + name + ".sql").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível carregar o relatório " + name, ex);
        }
    }
}
