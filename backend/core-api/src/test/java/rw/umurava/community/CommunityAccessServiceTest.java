package rw.umurava.community;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.umurava.identity.AuditService;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityAccessServiceTest {
    @Mock CommunityMembershipRepository memberships;
    @Mock AuditService audit;
    CommunityAccessService service;
    UUID actorId;
    UUID communityId;
    UUID targetId;

    @BeforeEach
    void setUp() {
        service = new CommunityAccessService(memberships, audit);
        actorId = UUID.randomUUID();
        communityId = UUID.randomUUID();
        targetId = UUID.randomUUID();
    }

    @Test
    void nonAdministratorCannotListCommunityMembers() {
        when(memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(
            actorId, communityId, MembershipRole.COMMUNITY_ADMIN.name(), "ACTIVE"
        )).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.listMembers(actorId, communityId));

        assertEquals(403, error.getStatusCode().value());
        verify(memberships, never()).findAllByCommunityIdAndStatus(any(), any());
    }

    @Test
    void communityAdministratorCannotPromoteAnotherCommunityAdministrator() {
        when(memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(
            actorId, communityId, MembershipRole.COMMUNITY_ADMIN.name(), "ACTIVE"
        )).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.addMember(actorId, communityId, targetId, MembershipRole.COMMUNITY_ADMIN));

        assertEquals(403, error.getStatusCode().value());
        verify(memberships, never()).save(any());
    }

    @Test
    void cannotRemoveLastCommunityAdministrator() {
        UUID membershipId = UUID.randomUUID();
        CommunityMembership admin = new CommunityMembership(actorId, communityId, MembershipRole.COMMUNITY_ADMIN);
        when(memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(
            actorId, communityId, MembershipRole.COMMUNITY_ADMIN.name(), "ACTIVE"
        )).thenReturn(true);
        when(memberships.findById(membershipId)).thenReturn(Optional.of(admin));
        when(memberships.countByCommunityIdAndRoleAndStatus(
            communityId, MembershipRole.COMMUNITY_ADMIN.name(), "ACTIVE"
        )).thenReturn(1L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.removeMember(actorId, communityId, membershipId));

        assertEquals(409, error.getStatusCode().value());
        assertEquals("ACTIVE", admin.getStatus());
    }
}
