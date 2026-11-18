package rw.umurava.community;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CommunityMembershipController {
    private final CommunityAccessService access;

    public CommunityMembershipController(CommunityAccessService access) {
        this.access = access;
    }

    public record AddMemberRequest(@NotNull UUID userId, @NotNull MembershipRole role) {}

    public record MembershipResponse(UUID id, UUID userId, UUID communityId, MembershipRole role, String status) {
        static MembershipResponse from(CommunityMembership m) {
            return new MembershipResponse(m.getId(), m.getUserId(), m.getCommunityId(), m.getRole(), m.getStatus());
        }
    }

    @GetMapping("/me/communities")
    public List<MembershipResponse> myCommunities(Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        return access.myMemberships(actorId).stream().map(MembershipResponse::from).toList();
    }

    @GetMapping("/communities/{communityId}/memberships")
    public List<MembershipResponse> listMembers(@PathVariable UUID communityId, Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        return access.listMembers(actorId, communityId).stream().map(MembershipResponse::from).toList();
    }

    @PostMapping("/communities/{communityId}/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse addMember(@PathVariable UUID communityId,
                                        @Valid @RequestBody AddMemberRequest request,
                                        Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        return MembershipResponse.from(access.addMember(actorId, communityId, request.userId(), request.role()));
    }

    @DeleteMapping("/communities/{communityId}/memberships/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID communityId, @PathVariable UUID membershipId,
                             Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        access.removeMember(actorId, communityId, membershipId);
    }
}
