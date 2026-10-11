SELECT COUNT(*) AS total_tickets,
       COUNT(*) FILTER (WHERE status IN ('OPEN', 'IN_PROGRESS')) AS open_backlog,
       COUNT(*) FILTER (WHERE status IN ('RESOLVED', 'CLOSED')) AS resolved_tickets,
       COUNT(*) FILTER (WHERE status IN ('OPEN', 'IN_PROGRESS') AND due_at < CURRENT_TIMESTAMP) AS overdue_backlog,
       ROUND(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600)
             FILTER (WHERE status IN ('RESOLVED', 'CLOSED')), 2) AS average_resolution_hours
FROM tickets
WHERE archived_at IS NULL;
