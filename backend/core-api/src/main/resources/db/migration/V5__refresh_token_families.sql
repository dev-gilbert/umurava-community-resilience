ALTER TABLE refresh_sessions
    ADD COLUMN family_id UUID,
    ADD COLUMN replaced_by UUID;

UPDATE refresh_sessions
SET family_id = id
WHERE family_id IS NULL;

ALTER TABLE refresh_sessions
    ALTER COLUMN family_id SET NOT NULL;

CREATE INDEX idx_refresh_sessions_family_active
    ON refresh_sessions(family_id)
    WHERE revoked_at IS NULL;
