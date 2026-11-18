CREATE TABLE community_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    label VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, community_id, id)
);

ALTER TABLE sync_events
    ADD COLUMN server_sequence BIGSERIAL;

CREATE UNIQUE INDEX idx_sync_events_server_sequence
    ON sync_events(server_sequence);

CREATE INDEX idx_sync_events_community_sequence
    ON sync_events(community_id, server_sequence);

CREATE TABLE sync_event_acknowledgements (
    event_id UUID NOT NULL REFERENCES sync_events(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES community_devices(id) ON DELETE CASCADE,
    acknowledged_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(event_id, device_id)
);

CREATE INDEX idx_sync_ack_device_time
    ON sync_event_acknowledgements(device_id, acknowledged_at);
