SELECT u.id AS agent_id, u.name AS agent_name,
       COUNT(t.id) AS assigned_tickets,
       COUNT(t.id) FILTER (WHERE t.status IN ('OPEN', 'IN_PROGRESS')) AS open_backlog,
       COUNT(t.id) FILTER (WHERE t.status IN ('RESOLVED', 'CLOSED')) AS resolved_tickets
FROM app_users u
LEFT JOIN tickets t ON t.assigned_agent_id = u.id AND t.archived_at IS NULL
WHERE u.role = 'AGENT' AND u.active = TRUE
GROUP BY u.id, u.name
ORDER BY assigned_tickets DESC, u.id;
