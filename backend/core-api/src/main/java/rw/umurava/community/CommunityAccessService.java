package rw.umurava.community;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.umurava.identity.AuditService;
import java.util.List;
import java.util.UUID;

@Service
public class CommunityAccessService {
    private static final String ACTIVE = "ACTIVE";
    private final CommunityMembershipRepository memberships;
    private final AuditService audit;

    public CommunityAccessService(CommunityMembershipRepository memberships, AuditService audit) {
        this.memberships = memberships;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CommunityMembership> myMemberships(UUID userId) {
        return memberships.findAllByUserIdAndStatus(userId, ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<CommunityMembership> listMembers(UUID actorId, UUID communityId) {
        requireCommunityAdmin(actorId, communityId);
        return memberships.findAllByCommunityIdAndStatus(communityId, ACTIVE);
    }

    @Transactional
    public CommunityMembership addMember(UUID actorId, UUID communityId, UUID targetUserId, MembershipRole role) {
        requireCommunityAdmin(actorId, communityId);
        if (role == MembershipRole.COMMUNITY_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Community administrators cannot appoint another community administrator");
        }
        if (memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(
                targetUserId, communityId, role.name(), ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This active membership already exists");
        }
        CommunityMembership saved = memberships.save(new CommunityMembership(targetUserId, communityId, role));
        audit.record(actorId, "COMMUNITY_MEMBER_ADDED", "COMMUNITY_MEMBERSHIP", saved.getId().toString(), "SUCCESS");
        return saved;
    }

    @Transactional
    public void removeMember(UUID actorId, UUID communityId, UUID membershipId) {
        requireCommunityAdmin(actorId, communityId);
        CommunityMembership membership = memberships.findById(membershipId)
            .filter(m -> m.getCommunityId().equals(communityId) && ACTIVE.equals(m.getStatus()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        if (membership.getRole() == MembershipRole.COMMUNITY_ADMIN
                && memberships.countByCommunityIdAndRoleAndStatus(communityId, MembershipRole.COMMUNITY_ADMIN.name(), ACTIVE) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A community must retain at least one administrator");
        }
        membership.deactivate();
        audit.record(actorId, "COMMUNITY_MEMBER_REMOVED", "COMMUNITY_MEMBERSHIP", membershipId.toString(), "SUCCESS");
    }

    private void requireCommunityAdmin(UUID actorId, UUID communityId) {
        boolean allowed = memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(
            actorId, communityId, MembershipRole.COMMUNITY_ADMIN.name(), ACTIVE);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Community administrator role required");
        }
    }
}
