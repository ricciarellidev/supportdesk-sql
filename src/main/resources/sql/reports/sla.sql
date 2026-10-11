WITH priorities(priority, sort_order) AS (
    VALUES ('CRITICAL', 1), ('HIGH', 2), ('MEDIUM', 3), ('LOW', 4)
)
SELECT p.priority, COUNT(t.id) AS resolved_tickets,
       COUNT(t.id) FILTER (WHERE t.resolved_at <= t.due_at) AS within_sla,
       ROUND(100.0 * COUNT(t.id) FILTER (WHERE t.resolved_at <= t.due_at)
             / NULLIF(COUNT(t.id), 0), 2) AS compliance_percent
FROM priorities p
LEFT JOIN tickets t ON t.priority = p.priority AND t.status IN ('RESOLVED', 'CLOSED') AND t.archived_at IS NULL
GROUP BY p.priority, p.sort_order
ORDER BY p.sort_order;
