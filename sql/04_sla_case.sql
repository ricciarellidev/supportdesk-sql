-- Denominator: only resolved/closed, non-archived tickets. Open tickets do not count as failed resolutions.
SELECT priority, COUNT(*) AS resolved_tickets,
       SUM(CASE WHEN resolved_at <= due_at THEN 1 ELSE 0 END) AS within_sla,
       ROUND(100.0 * SUM(CASE WHEN resolved_at <= due_at THEN 1 ELSE 0 END) / COUNT(*), 2) AS compliance_percent
FROM tickets
WHERE status IN ('RESOLVED', 'CLOSED') AND archived_at IS NULL
GROUP BY priority
ORDER BY priority;
