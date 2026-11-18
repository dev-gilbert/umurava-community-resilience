ALTER TABLE community_memberships
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_memberships_community_status
    ON community_memberships(community_id, status);

CREATE INDEX idx_memberships_user_community_status
    ON community_memberships(user_id, community_id, status);
