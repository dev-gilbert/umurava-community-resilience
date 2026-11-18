package rw.umurava.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {
    private final SyncService sync;

    public SyncController(SyncService sync) {
        this.sync = sync;
    }

    public record RegisterDeviceRequest(@NotNull UUID communityId, @NotNull UUID deviceId,
                                        @NotBlank @Size(max = 120) String label) {}
    public record PushRequest(@NotNull UUID communityId, @NotNull UUID deviceId,
                              @Size(min = 1, max = 100) List<@Valid SyncEventInput> events) {}
    public record PullRequest(@NotNull UUID communityId, @NotNull UUID deviceId,
                              long cursor, int limit) {}
    public record AckRequest(@NotNull UUID communityId, @NotNull UUID deviceId,
                             @Size(min = 1, max = 200) List<@NotNull UUID> eventIds) {}

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public SyncService.DeviceResult registerDevice(@Valid @RequestBody RegisterDeviceRequest request,
                                                    Authentication authentication) {
        return sync.registerDevice(userId(authentication), request.communityId(), request.deviceId(), request.label());
    }

    @PostMapping("/push")
    public List<SyncService.PushResult> push(@Valid @RequestBody PushRequest request,
                                             Authentication authentication) {
        return sync.push(userId(authentication), request.communityId(), request.deviceId(), request.events());
    }

    @GetMapping("/pull")
    public SyncService.PullResult pull(@RequestParam UUID communityId, @RequestParam UUID deviceId,
                                       @RequestParam(defaultValue = "0") long cursor,
                                       @RequestParam(defaultValue = "100") int limit,
                                       Authentication authentication) {
        return sync.pull(userId(authentication), communityId, deviceId, cursor, limit);
    }

    @PostMapping("/ack")
    public Map<String, Integer> acknowledge(@Valid @RequestBody AckRequest request,
                                            Authentication authentication) {
        int accepted = sync.acknowledge(userId(authentication), request.communityId(),
            request.deviceId(), request.eventIds());
        return Map.of("acknowledged", accepted);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
