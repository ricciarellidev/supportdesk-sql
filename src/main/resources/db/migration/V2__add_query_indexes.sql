-- Index names describe the access pattern. Small demo tables may still use a sequential scan.
CREATE INDEX idx_tickets_active_created ON tickets (created_at DESC, id DESC) WHERE archived_at IS NULL;
CREATE INDEX idx_tickets_active_status ON tickets (status, created_at DESC, id DESC) WHERE archived_at IS NULL;
CREATE INDEX idx_tickets_active_category ON tickets (category_id) WHERE archived_at IS NULL;
CREATE INDEX idx_tickets_active_agent ON tickets (assigned_agent_id) WHERE archived_at IS NULL;
CREATE INDEX idx_tickets_requester ON tickets (requester_id);
CREATE INDEX idx_tickets_open_deadline ON tickets (due_at) WHERE status IN ('OPEN', 'IN_PROGRESS') AND archived_at IS NULL;
CREATE INDEX idx_events_ticket_order ON ticket_events (ticket_id, created_at, id);
CREATE INDEX idx_users_department ON app_users (department_id);
CREATE INDEX idx_events_actor ON ticket_events (actor_id);
