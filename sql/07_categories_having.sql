-- WHERE filters rows; HAVING filters aggregated groups.
SELECT c.id, c.name, COUNT(t.id) AS ticket_count
FROM categories c JOIN tickets t ON t.category_id = c.id
WHERE t.archived_at IS NULL
GROUP BY c.id, c.name
HAVING COUNT(t.id) >= 2
ORDER BY ticket_count DESC, c.id;
