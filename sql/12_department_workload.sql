-- Distinguish the requester's department from the category's responsible department.
SELECT d.id, d.name AS requester_department, COUNT(t.id) AS requested_tickets,
       COUNT(t.id) FILTER (WHERE t.status IN ('OPEN', 'IN_PROGRESS')) AS backlog
FROM departments d
LEFT JOIN app_users u ON u.department_id = d.id AND u.role = 'REQUESTER'
LEFT JOIN tickets t ON t.requester_id = u.id AND t.archived_at IS NULL
GROUP BY d.id, d.name
ORDER BY requested_tickets DESC, d.id;
