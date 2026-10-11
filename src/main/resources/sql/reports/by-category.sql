SELECT c.id AS category_id, c.name AS category_name,
       COUNT(t.id) AS total_tickets,
       COUNT(t.id) FILTER (WHERE t.status IN ('OPEN', 'IN_PROGRESS')) AS open_backlog,
       ROUND(AVG(EXTRACT(EPOCH FROM (t.resolved_at - t.created_at)) / 3600)
             FILTER (WHERE t.status IN ('RESOLVED', 'CLOSED')), 2) AS average_resolution_hours
FROM categories c
LEFT JOIN tickets t ON t.category_id = c.id AND t.archived_at IS NULL
GROUP BY c.id, c.name
ORDER BY total_tickets DESC, c.id;
