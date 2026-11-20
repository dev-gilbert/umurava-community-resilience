package rw.umurava.sync;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import rw.umurava.community.CommunityMembershipRepository;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SyncProcessingServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CommunityMembershipRepository memberships = mock(CommunityMembershipRepository.class);
    private final SyncProcessingService service = new SyncProcessingService(jdbc, memberships);

    @Test
    void processingRequiresCommunityAdministratorOrResourceManager() {
        UUID user=UUID.randomUUID(), community=UUID.randomUUID();
        when(memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(user,community,"COMMUNITY_ADMIN","ACTIVE")).thenReturn(false);
        when(memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(user,community,"RESOURCE_MANAGER","ACTIVE")).thenReturn(false);
        ResponseStatusException ex=assertThrows(ResponseStatusException.class,
            ()->service.process(user,community,50));
        assertEquals(403,ex.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void conflictDecisionIsValidatedBeforeDatabaseAccess() {
        UUID user=UUID.randomUUID(), community=UUID.randomUUID(), conflict=UUID.randomUUID();
        when(memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(user,community,"COMMUNITY_ADMIN","ACTIVE")).thenReturn(true);
        ResponseStatusException ex=assertThrows(ResponseStatusException.class,
            ()->service.resolve(user,community,conflict,"LAST_WRITE_WINS"));
        assertEquals(400,ex.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void deviceRevocationRequiresAuthorizedMembership() {
        UUID user=UUID.randomUUID(), community=UUID.randomUUID(), device=UUID.randomUUID();
        when(memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(user,community,"COMMUNITY_ADMIN","ACTIVE")).thenReturn(true);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        assertDoesNotThrow(()->service.revokeDevice(user,community,device,"Device reported lost"));
        verify(jdbc).update(contains("status='REVOKED'"), any(Object[].class));
    }
}
