-- Keep active agents with zero tickets. Inactive agents are deliberately excluded.
SELECT u.id, u.name, COUNT(t.id) AS assigned_tickets
FROM app_users u
LEFT JOIN tickets t ON t.assigned_agent_id = u.id AND t.archived_at IS NULL
WHERE u.role = 'AGENT' AND u.active = TRUE
GROUP BY u.id, u.name
ORDER BY assigned_tickets DESC, u.id;
