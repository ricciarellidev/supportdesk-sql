-- Explicit UTC avoids grouping dates according to the database session's time zone.
WITH daily_volume AS (
    SELECT (created_at AT TIME ZONE 'UTC')::date AS day, COUNT(*) AS opened_tickets
    FROM tickets WHERE archived_at IS NULL
    GROUP BY (created_at AT TIME ZONE 'UTC')::date
)
SELECT day, opened_tickets FROM daily_volume ORDER BY day;
