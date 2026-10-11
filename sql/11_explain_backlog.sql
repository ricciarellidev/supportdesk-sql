-- Analyze a SELECT only. Small tables may prefer a sequential scan; an index is not a guaranteed speedup.
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, due_at FROM tickets
WHERE status IN ('OPEN', 'IN_PROGRESS') AND archived_at IS NULL AND due_at < CURRENT_TIMESTAMP
ORDER BY due_at, id;
