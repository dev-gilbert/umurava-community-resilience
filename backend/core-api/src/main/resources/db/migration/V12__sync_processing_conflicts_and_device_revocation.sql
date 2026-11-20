ALTER TABLE community_devices
    ADD COLUMN revoked_at TIMESTAMPTZ,
    ADD COLUMN revoked_by UUID REFERENCES app_users(id),
    ADD COLUMN revocation_reason VARCHAR(240);

ALTER TABLE sync_events
    ADD COLUMN processed_at TIMESTAMPTZ,
    ADD COLUMN processing_error VARCHAR(500);

CREATE TABLE sync_entity_state (
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    version BIGINT NOT NULL CHECK (version > 0),
    payload JSONB NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    last_event_id UUID NOT NULL UNIQUE REFERENCES sync_events(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (community_id, entity_type, entity_id)
);

CREATE TABLE sync_conflicts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE REFERENCES sync_events(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    expected_version BIGINT NOT NULL,
    received_version BIGINT NOT NULL,
    incoming_payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED','REJECTED')),
    resolution_payload JSONB,
    resolved_by UUID REFERENCES app_users(id),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sync_conflicts_community_status ON sync_conflicts(community_id,status,created_at);
CREATE INDEX idx_sync_events_processing ON sync_events(status,server_sequence);

CREATE TABLE edge_peers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    peer_code VARCHAR(80) NOT NULL UNIQUE,
    endpoint_url TEXT NOT NULL,
    credential_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
    last_success_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE edge_sync_cursors (
    peer_id UUID PRIMARY KEY REFERENCES edge_peers(id) ON DELETE CASCADE,
    last_cloud_sequence BIGINT NOT NULL DEFAULT 0,
    last_edge_sequence BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
