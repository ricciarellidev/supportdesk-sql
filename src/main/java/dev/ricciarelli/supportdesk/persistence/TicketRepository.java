package dev.ricciarelli.supportdesk.persistence;

import dev.ricciarelli.supportdesk.domain.Priority;
import dev.ricciarelli.supportdesk.domain.TicketStatus;
import dev.ricciarelli.supportdesk.web.model.TicketModels.*;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TicketRepository {
    private static final String FROM =
            """
            FROM tickets t
            JOIN app_users requester ON requester.id = t.requester_id
            JOIN categories c ON c.id = t.category_id
            JOIN departments d ON d.id = c.department_id
            LEFT JOIN app_users agent ON agent.id = t.assigned_agent_id
            """;
    private static final String SELECT =
            """
            SELECT t.*, requester.name AS requester_name, c.name AS category_name,
                   d.id AS department_id, d.name AS department_name, agent.name AS agent_name
            """
                    + FROM;
    private static final String FILTER =
            """
            WHERE t.archived_at IS NULL
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:categoryId IS NULL OR t.category_id = :categoryId)
              AND (:agentId IS NULL OR t.assigned_agent_id = :agentId)
              AND (:query IS NULL OR t.title ILIKE :query ESCAPE '\\')
            """;
    private static final RowMapper<TicketView> TICKET_MAPPER =
            (rs, row) ->
                    new TicketView(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getLong("requester_id"),
                            rs.getString("requester_name"),
                            rs.getLong("category_id"),
                            rs.getString("category_name"),
                            rs.getLong("department_id"),
                            rs.getString("department_name"),
                            rs.getObject("assigned_agent_id", Long.class),
                            rs.getString("agent_name"),
                            Priority.valueOf(rs.getString("priority")),
                            TicketStatus.valueOf(rs.getString("status")),
                            rs.getObject("created_at", OffsetDateTime.class),
                            rs.getObject("updated_at", OffsetDateTime.class),
                            rs.getObject("due_at", OffsetDateTime.class),
                            rs.getObject("resolved_at", OffsetDateTime.class),
                            rs.getObject("closed_at", OffsetDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    public TicketRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<TicketView> find(long id) {
        return jdbc
                .query(
                        SELECT + "WHERE t.id = :id AND t.archived_at IS NULL",
                        Map.of("id", id),
                        TICKET_MAPPER)
                .stream()
                .findFirst();
    }

    public boolean lock(long id) {
        return !jdbc.query(
                        "SELECT id FROM tickets WHERE id = :id AND archived_at IS NULL FOR UPDATE",
                        Map.of("id", id),
                        (rs, row) -> rs.getLong("id"))
                .isEmpty();
    }

    public TicketPage list(TicketFilter filter) {
        String query =
                filter.query() == null || filter.query().isBlank()
                        ? null
                        : "%"
                                + filter.query()
                                        .trim()
                                        .replace("\\", "\\\\")
                                        .replace("%", "\\%")
                                        .replace("_", "\\_")
                                + "%";
        var params =
                new MapSqlParameterSource()
                        .addValue(
                                "status",
                                filter.status() == null ? null : filter.status().name(),
                                Types.VARCHAR)
                        .addValue(
                                "priority",
                                filter.priority() == null ? null : filter.priority().name(),
                                Types.VARCHAR)
                        .addValue("categoryId", filter.categoryId(), Types.BIGINT)
                        .addValue("agentId", filter.agentId(), Types.BIGINT)
                        .addValue("query", query, Types.VARCHAR)
                        .addValue("size", filter.size())
                        .addValue("offset", filter.page() * filter.size());
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + FROM + FILTER, params, Long.class);
        List<TicketView> content =
                jdbc.query(
                        SELECT
                                + FILTER
                                + "ORDER BY t.created_at DESC, t.id DESC LIMIT :size OFFSET"
                                + " :offset",
                        params,
                        TICKET_MAPPER);
        return new TicketPage(
                content,
                total,
                filter.page(),
                filter.size(),
                (total + filter.size() - 1) / filter.size());
    }

    public long create(CreateTicket request, OffsetDateTime now, OffsetDateTime deadline) {
        Long id =
                jdbc.queryForObject(
                        """
INSERT INTO tickets (title, description, requester_id, category_id, priority, created_at, updated_at, due_at)
VALUES (:title, :description, :requesterId, :categoryId, :priority, :now, :now, :deadline) RETURNING id
""",
                        new MapSqlParameterSource()
                                .addValue("title", request.title().trim())
                                .addValue("description", request.description().trim())
                                .addValue("requesterId", request.requesterId())
                                .addValue("categoryId", request.categoryId())
                                .addValue("priority", request.priority().name())
                                .addValue("now", now)
                                .addValue("deadline", deadline),
                        Long.class);
        return id;
    }

    public void assign(long id, long agentId, OffsetDateTime now) {
        jdbc.update(
                "UPDATE tickets SET assigned_agent_id = :agentId, updated_at = :now WHERE id = :id",
                Map.of("id", id, "agentId", agentId, "now", now));
    }

    public void transition(
            long id,
            TicketStatus status,
            OffsetDateTime resolvedAt,
            OffsetDateTime closedAt,
            OffsetDateTime now) {
        jdbc.update(
                """
UPDATE tickets SET status = :status, resolved_at = :resolvedAt, closed_at = :closedAt, updated_at = :now
WHERE id = :id
""",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("status", status.name())
                        .addValue("resolvedAt", resolvedAt, Types.TIMESTAMP_WITH_TIMEZONE)
                        .addValue("closedAt", closedAt, Types.TIMESTAMP_WITH_TIMEZONE)
                        .addValue("now", now));
    }

    public void archive(long id, OffsetDateTime now) {
        jdbc.update(
                "UPDATE tickets SET archived_at = :now, updated_at = :now WHERE id = :id",
                Map.of("id", id, "now", now));
    }

    public void addEvent(
            long ticketId,
            long actorId,
            String type,
            TicketStatus previous,
            TicketStatus next,
            String message,
            OffsetDateTime now) {
        jdbc.update(
                """
INSERT INTO ticket_events (ticket_id, actor_id, event_type, previous_status, new_status, message, created_at)
VALUES (:ticketId, :actorId, :type, :previous, :next, :message, :now)
""",
                new MapSqlParameterSource()
                        .addValue("ticketId", ticketId)
                        .addValue("actorId", actorId)
                        .addValue("type", type)
                        .addValue(
                                "previous",
                                previous == null ? null : previous.name(),
                                Types.VARCHAR)
                        .addValue("next", next == null ? null : next.name(), Types.VARCHAR)
                        .addValue("message", message)
                        .addValue("now", now));
    }

    public List<EventView> history(long id) {
        return jdbc.query(
                """
SELECT e.*, u.name AS actor_name FROM ticket_events e JOIN app_users u ON u.id = e.actor_id
WHERE e.ticket_id = :id ORDER BY e.created_at, e.id
""",
                Map.of("id", id),
                (rs, row) ->
                        new EventView(
                                rs.getLong("id"),
                                rs.getLong("actor_id"),
                                rs.getString("actor_name"),
                                rs.getString("event_type"),
                                rs.getString("previous_status"),
                                rs.getString("new_status"),
                                rs.getString("message"),
                                rs.getObject("created_at", OffsetDateTime.class)));
    }
}
