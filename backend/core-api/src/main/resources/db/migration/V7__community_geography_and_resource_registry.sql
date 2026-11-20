ALTER TABLE communities ADD COLUMN province_name VARCHAR(120), ADD COLUMN community_code VARCHAR(40), ADD COLUMN description TEXT, ADD COLUMN latitude DOUBLE PRECISION, ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE communities ADD CONSTRAINT chk_community_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90), ADD CONSTRAINT chk_community_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);
CREATE UNIQUE INDEX uq_communities_code ON communities(community_code) WHERE community_code IS NOT NULL;
CREATE INDEX idx_communities_geography ON communities(province_name,district_name,sector_name,cell_name,village_name);
ALTER TABLE resources ADD COLUMN description TEXT, ADD COLUMN minimum_quantity NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (minimum_quantity >= 0);
CREATE INDEX idx_resources_community_updated ON resources(community_id,updated_at DESC);
CREATE INDEX idx_resource_transactions_resource_time ON resource_transactions(resource_id,created_at DESC);
