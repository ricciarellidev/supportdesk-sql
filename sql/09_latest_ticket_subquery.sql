-- Correlated anti-subquery: the latest ticket for each requester, with ID as a tie-breaker.
SELECT t.id, t.requester_id, t.title, t.created_at
FROM tickets t
WHERE t.archived_at IS NULL AND NOT EXISTS (
    SELECT 1 FROM tickets newer
    WHERE newer.requester_id = t.requester_id AND newer.archived_at IS NULL
      AND (newer.created_at > t.created_at OR (newer.created_at = t.created_at AND newer.id > t.id))
)
ORDER BY t.requester_id;
