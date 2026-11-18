package rw.umurava.community;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_memberships")
public class CommunityMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(nullable = false, length = 40)
    private String role;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CommunityMembership() {}

    public CommunityMembership(UUID userId, UUID communityId, MembershipRole role) {
        this.userId = userId;
        this.communityId = communityId;
        this.role = role.name();
        this.status = "ACTIVE";
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCommunityId() { return communityId; }
    public MembershipRole getRole() { return MembershipRole.valueOf(role); }
    public String getStatus() { return status; }
    public void deactivate() { this.status = "INACTIVE"; }
}
