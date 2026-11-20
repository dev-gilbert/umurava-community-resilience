package rw.umurava.sync;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.umurava.community.CommunityMembershipRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SyncProcessingService {
    private static final List<String> MODULE_TYPES = List.of(
        "COMMUNITY","RESOURCE","FOOD_BATCH","WATER_SOURCE","WATER_SCREENING",
        "ENERGY_ASSET","HEALTH_FACILITY","TRANSPORT_VEHICLE","TRANSPORT_REQUEST",
        "INCIDENT","ANNOUNCEMENT"
    );
    private final JdbcTemplate jdbc;
    private final CommunityMembershipRepository memberships;

    public SyncProcessingService(JdbcTemplate jdbc, CommunityMembershipRepository memberships) {
        this.jdbc = jdbc;
        this.memberships = memberships;
    }

    public record ProcessResult(int applied, int conflicted, int rejected) {}
    public record ConflictView(UUID id, UUID eventId, String entityType, UUID entityId,
                               long expectedVersion, long receivedVersion, String status) {}

    @Transactional
    public ProcessResult process(UUID userId, UUID communityId, int limit) {
        requireAdminOrManager(userId, communityId);
        List<Map<String,Object>> events = jdbc.queryForList("""
            SELECT e.id,e.entity_type,e.entity_id,e.operation,e.payload::text AS payload,
                   e.event_version,e.device_id
            FROM sync_events e
            JOIN community_devices d ON d.id=e.device_id AND d.status='ACTIVE'
            WHERE e.community_id=? AND e.status='RECEIVED'
            ORDER BY e.server_sequence
            FOR UPDATE OF e SKIP LOCKED LIMIT ?
            """, communityId, Math.max(1, Math.min(limit, 200)));
        int applied=0, conflicted=0, rejected=0;
        for (Map<String,Object> e : events) {
            UUID eventId=(UUID)e.get("id");
            String type=(String)e.get("entity_type");
            String operation=(String)e.get("operation");
            UUID entityId=(UUID)e.get("entity_id");
            long incoming=((Number)e.get("event_version")).longValue();
            String payload=(String)e.get("payload");
            if (!MODULE_TYPES.contains(type) || !List.of("UPSERT","DELETE").contains(operation)) {
                jdbc.update("UPDATE sync_events SET status='REJECTED',processing_error='Unsupported entity type or operation',processed_at=NOW() WHERE id=?",eventId);
                rejected++; continue;
            }
            List<Map<String,Object>> current=jdbc.queryForList("""
                SELECT version FROM sync_entity_state WHERE community_id=? AND entity_type=? AND entity_id=? FOR UPDATE
                """,communityId,type,entityId);
            long expected=current.isEmpty()?1L:((Number)current.getFirst().get("version")).longValue()+1L;
            if (incoming!=expected) {
                jdbc.update("""
                    INSERT INTO sync_conflicts(event_id,community_id,entity_type,entity_id,expected_version,received_version,incoming_payload)
                    VALUES (?,?,?,?,?,?,CAST(? AS jsonb)) ON CONFLICT(event_id) DO NOTHING
                    """,eventId,communityId,type,entityId,expected,incoming,payload);
                jdbc.update("UPDATE sync_events SET status='CONFLICT',processed_at=NOW(),processing_error=? WHERE id=?",
                    "Expected version "+expected+" but received "+incoming,eventId);
                conflicted++; continue;
            }
            jdbc.update("""
                INSERT INTO sync_entity_state(community_id,entity_type,entity_id,version,payload,deleted,last_event_id)
                VALUES (?,?,?, ?,CAST(? AS jsonb),?,?)
                ON CONFLICT(community_id,entity_type,entity_id) DO UPDATE SET
                  version=EXCLUDED.version,payload=EXCLUDED.payload,deleted=EXCLUDED.deleted,
                  last_event_id=EXCLUDED.last_event_id,updated_at=NOW()
                """,communityId,type,entityId,incoming,payload,"DELETE".equals(operation),eventId);
            jdbc.update("UPDATE sync_events SET status='APPLIED',processed_at=NOW(),processing_error=NULL WHERE id=?",eventId);
            applied++;
        }
        return new ProcessResult(applied,conflicted,rejected);
    }

    @Transactional(readOnly=true)
    public List<ConflictView> conflicts(UUID userId, UUID communityId) {
        requireAdminOrManager(userId,communityId);
        return jdbc.query("""
            SELECT id,event_id,entity_type,entity_id,expected_version,received_version,status
            FROM sync_conflicts WHERE community_id=? AND status='OPEN' ORDER BY created_at
            """,(rs,n)->new ConflictView(rs.getObject("id",UUID.class),rs.getObject("event_id",UUID.class),
            rs.getString("entity_type"),rs.getObject("entity_id",UUID.class),rs.getLong("expected_version"),
            rs.getLong("received_version"),rs.getString("status")),communityId);
    }

    @Transactional
    public void resolve(UUID userId, UUID communityId, UUID conflictId, String decision) {
        requireAdminOrManager(userId,communityId);
        if (!List.of("ACCEPT_INCOMING","REJECT_INCOMING").contains(decision))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported conflict decision");
        List<Map<String,Object>> rows=jdbc.queryForList("""
            SELECT c.event_id,c.entity_type,c.entity_id,c.received_version,c.incoming_payload::text AS payload
            FROM sync_conflicts c WHERE c.id=? AND c.community_id=? AND c.status='OPEN' FOR UPDATE
            """,conflictId,communityId);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Open conflict not found");
        Map<String,Object> c=rows.getFirst();
        UUID eventId=(UUID)c.get("event_id");
        if("ACCEPT_INCOMING".equals(decision)) {
            jdbc.update("""
                INSERT INTO sync_entity_state(community_id,entity_type,entity_id,version,payload,deleted,last_event_id)
                VALUES (?,?,?,?,CAST(? AS jsonb),FALSE,?)
                ON CONFLICT(community_id,entity_type,entity_id) DO UPDATE SET
                  version=EXCLUDED.version,payload=EXCLUDED.payload,deleted=FALSE,last_event_id=EXCLUDED.last_event_id,updated_at=NOW()
                """,communityId,c.get("entity_type"),c.get("entity_id"),c.get("received_version"),c.get("payload"),eventId);
            jdbc.update("UPDATE sync_events SET status='APPLIED',processing_error=NULL WHERE id=?",eventId);
        } else jdbc.update("UPDATE sync_events SET status='REJECTED' WHERE id=?",eventId);
        jdbc.update("UPDATE sync_conflicts SET status=?,resolved_by=?,resolved_at=NOW() WHERE id=?",
            "ACCEPT_INCOMING".equals(decision)?"RESOLVED":"REJECTED",userId,conflictId);
    }

    @Transactional
    public void revokeDevice(UUID actor, UUID communityId, UUID deviceId, String reason) {
        requireAdminOrManager(actor,communityId);
        int changed=jdbc.update("""
            UPDATE community_devices SET status='REVOKED',revoked_at=NOW(),revoked_by=?,revocation_reason=?
            WHERE id=? AND community_id=? AND status='ACTIVE'
            """,actor,reason,deviceId,communityId);
        if(changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active device not found");
    }

    private void requireAdminOrManager(UUID userId, UUID communityId) {
        boolean allowed=memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(userId,communityId,"COMMUNITY_ADMIN","ACTIVE");
        if(!allowed) allowed=memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(userId,communityId,"RESOURCE_MANAGER","ACTIVE");
        if(!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Community administrator permission required");
    }
}
