-- Question: who requested each unresolved ticket, and who is responsible?
SELECT t.id, t.title, requester.name AS requester, c.name AS category,
       COALESCE(agent.name, 'Sem responsável') AS agent, t.priority, t.status, t.due_at
FROM tickets t
JOIN app_users requester ON requester.id = t.requester_id
JOIN categories c ON c.id = t.category_id
LEFT JOIN app_users agent ON agent.id = t.assigned_agent_id
WHERE t.status IN ('OPEN', 'IN_PROGRESS') AND t.archived_at IS NULL
ORDER BY t.due_at, t.id;
