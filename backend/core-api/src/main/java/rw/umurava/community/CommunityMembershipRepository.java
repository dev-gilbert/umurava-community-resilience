package rw.umurava.community;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, UUID> {
    List<CommunityMembership> findAllByUserIdAndStatus(UUID userId, String status);
    List<CommunityMembership> findAllByCommunityIdAndStatus(UUID communityId, String status);
    Optional<CommunityMembership> findByUserIdAndCommunityIdAndRoleAndStatus(
        UUID userId, UUID communityId, String role, String status
    );
    boolean existsByUserIdAndCommunityIdAndRoleAndStatus(
        UUID userId, UUID communityId, String role, String status
    );
    long countByCommunityIdAndRoleAndStatus(UUID communityId, String role, String status);
}
