-- EXISTS answers whether a relationship exists, without duplicating the requester.
SELECT u.id, u.name
FROM app_users u
WHERE u.role = 'REQUESTER' AND EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.requester_id = u.id AND t.status IN ('OPEN', 'IN_PROGRESS') AND t.archived_at IS NULL
)
ORDER BY u.id;
