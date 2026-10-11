-- A window function ranks rows without collapsing the result set again.
WITH workload AS (
    SELECT u.id, u.name, COUNT(t.id) AS assigned_tickets
    FROM app_users u
    LEFT JOIN tickets t ON t.assigned_agent_id = u.id AND t.archived_at IS NULL
    WHERE u.role = 'AGENT' AND u.active = TRUE
    GROUP BY u.id, u.name
)
SELECT id, name, assigned_tickets,
       DENSE_RANK() OVER (ORDER BY assigned_tickets DESC) AS workload_rank
FROM workload ORDER BY workload_rank, id;
