-- COUNT(t.id), rather than COUNT(*), preserves zero for categories without tickets.
SELECT c.name AS category, COUNT(t.id) AS ticket_count,
       COUNT(t.id) FILTER (WHERE t.status IN ('OPEN', 'IN_PROGRESS')) AS backlog
FROM categories c
LEFT JOIN tickets t ON t.category_id = c.id AND t.archived_at IS NULL
GROUP BY c.id, c.name
ORDER BY ticket_count DESC, c.id;
