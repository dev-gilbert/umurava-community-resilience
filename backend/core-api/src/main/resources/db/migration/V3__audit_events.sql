CREATE TABLE audit_events (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 actor_user_id UUID REFERENCES app_users(id),
 action VARCHAR(80) NOT NULL,
 target_type VARCHAR(80) NOT NULL,
 target_id VARCHAR(120),
 outcome VARCHAR(30) NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_actor_time ON audit_events(actor_user_id,occurred_at DESC);
CREATE INDEX idx_audit_action_time ON audit_events(action,occurred_at DESC);
