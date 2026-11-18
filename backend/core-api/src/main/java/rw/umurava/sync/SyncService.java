package rw.umurava.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.umurava.community.CommunityMembershipRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SyncService {
    private static final String ACTIVE = "ACTIVE";
    private final JdbcTemplate jdbc;
    private final CommunityMembershipRepository memberships;
    private final ObjectMapper mapper;

    public SyncService(JdbcTemplate jdbc, CommunityMembershipRepository memberships, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.memberships = memberships;
        this.mapper = mapper;
    }

    public record DeviceResult(UUID deviceId, UUID communityId, String status) {}
    public record PushResult(UUID eventId, String result) {}
    public record SyncEventResult(UUID id, UUID communityId, UUID deviceId, String entityType,
                                  UUID entityId, String operation, JsonNode payload,
                                  long eventVersion, Instant eventCreatedAt, long serverSequence) {}
    public record PullResult(long nextCursor, boolean hasMore, List<SyncEventResult> events) {}

    @Transactional
    public DeviceResult registerDevice(UUID userId, UUID communityId, UUID deviceId, String label) {
        requireMembership(userId, communityId);
        var existing = jdbc.query(
            "SELECT user_id, community_id, status FROM community_devices WHERE id = ? FOR UPDATE",
            (rs, row) -> new Object[]{rs.getObject("user_id", UUID.class),
                rs.getObject("community_id", UUID.class), rs.getString("status")}, deviceId);
        if (!existing.isEmpty()) {
            Object[] row = existing.getFirst();
            if (!userId.equals(row[0]) || !communityId.equals(row[1])) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Device identifier is already registered");
            }
            if (!ACTIVE.equals(row[2])) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device is disabled");
            }
            jdbc.update("UPDATE community_devices SET last_seen_at = NOW(), label = ? WHERE id = ?", label, deviceId);
            return new DeviceResult(deviceId, communityId, ACTIVE);
        }
        jdbc.update("INSERT INTO community_devices(id,user_id,community_id,label) VALUES (?,?,?,?)",
            deviceId, userId, communityId, label);
        return new DeviceResult(deviceId, communityId, ACTIVE);
    }

    @Transactional
    public List<PushResult> push(UUID userId, UUID communityId, UUID deviceId, List<SyncEventInput> events) {
        requireMembership(userId, communityId);
        requireDevice(userId, communityId, deviceId);
        List<PushResult> results = new ArrayList<>();
        for (SyncEventInput event : events) {
            if (!event.deviceId().equals(deviceId) || !event.communityId().equals(communityId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Event device and community must match the sync request");
            }
            if (!List.of("UPSERT", "DELETE").contains(event.operation())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sync operation");
            }
            String payload;
            try {
                payload = mapper.writeValueAsString(event.payload());
            } catch (Exception exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event payload");
            }
            int inserted = jdbc.update("""
                INSERT INTO sync_events(id,community_id,device_id,entity_type,entity_id,operation,
                    payload,event_version,event_created_at,status)
                VALUES (?,?,?,?,?,?,CAST(? AS jsonb),?,?, 'RECEIVED')
                ON CONFLICT (id) DO NOTHING
                """, event.id(), communityId, deviceId, event.entityType(), event.entityId(),
                event.operation(), payload, event.eventVersion(), Timestamp.from(event.eventCreatedAt()));

            if (inserted == 1) {
                results.add(new PushResult(event.id(), "ACCEPTED"));
            } else {
                boolean same = jdbc.query("""
                    SELECT EXISTS(SELECT 1 FROM sync_events
                    WHERE id = ? AND community_id = ? AND device_id = ? AND entity_type = ?
                      AND entity_id = ? AND operation = ? AND payload = CAST(? AS jsonb)
                      AND event_version = ? AND event_created_at = ?)
                    """, rs -> rs.next() && rs.getBoolean(1), event.id(), communityId, deviceId,
                    event.entityType(), event.entityId(), event.operation(), payload,
                    event.eventVersion(), Timestamp.from(event.eventCreatedAt()));
                if (!same) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Event identifier was previously used with different content");
                }
                results.add(new PushResult(event.id(), "DUPLICATE"));
            }
        }
        jdbc.update("UPDATE community_devices SET last_seen_at = NOW() WHERE id = ?", deviceId);
        return results;
    }

    @Transactional(readOnly = true)
    public PullResult pull(UUID userId, UUID communityId, UUID deviceId, long cursor, int limit) {
        requireMembership(userId, communityId);
        requireDevice(userId, communityId, deviceId);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<SyncEventResult> events = jdbc.query("""
            SELECT id, community_id, device_id, entity_type, entity_id, operation,
                   payload::text AS payload_text, event_version, event_created_at, server_sequence
            FROM sync_events
            WHERE community_id = ? AND server_sequence > ?
            ORDER BY server_sequence ASC
            LIMIT ?
            """, (rs, row) -> new SyncEventResult(
                rs.getObject("id", UUID.class), rs.getObject("community_id", UUID.class),
                rs.getObject("device_id", UUID.class), rs.getString("entity_type"),
                rs.getObject("entity_id", UUID.class), rs.getString("operation"),
                mapper.readTree(rs.getString("payload_text")), rs.getLong("event_version"),
                rs.getTimestamp("event_created_at").toInstant(), rs.getLong("server_sequence")
            ), communityId, Math.max(0, cursor), safeLimit + 1);
        boolean hasMore = events.size() > safeLimit;
        if (hasMore) events = new ArrayList<>(events.subList(0, safeLimit));
        long nextCursor = events.isEmpty() ? Math.max(0, cursor) :
            events.get(events.size() - 1).serverSequence();
        return new PullResult(nextCursor, hasMore, events);
    }

    @Transactional
    public int acknowledge(UUID userId, UUID communityId, UUID deviceId, List<UUID> eventIds) {
        requireMembership(userId, communityId);
        requireDevice(userId, communityId, deviceId);
        int accepted = 0;
        for (UUID eventId : eventIds) {
            accepted += jdbc.update("""
                INSERT INTO sync_event_acknowledgements(event_id,device_id)
                SELECT ?, ? WHERE EXISTS (
                    SELECT 1 FROM sync_events WHERE id = ? AND community_id = ?
                )
                ON CONFLICT (event_id,device_id) DO NOTHING
                """, eventId, deviceId, eventId, communityId);
        }
        jdbc.update("UPDATE community_devices SET last_seen_at = NOW() WHERE id = ?", deviceId);
        return accepted;
    }

    private void requireMembership(UUID userId, UUID communityId) {
        if (!memberships.existsByUserIdAndCommunityIdAndStatus(userId, communityId, ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active community membership required");
        }
    }

    private void requireDevice(UUID userId, UUID communityId, UUID deviceId) {
        Boolean valid = jdbc.query("""
            SELECT EXISTS(SELECT 1 FROM community_devices
                WHERE id = ? AND user_id = ? AND community_id = ? AND status = 'ACTIVE')
            """, rs -> rs.next() && rs.getBoolean(1), deviceId, userId, communityId);
        if (!Boolean.TRUE.equals(valid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active device registration required");
        }
    }
}
