package rw.umurava.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncProcessingController {
    private final SyncProcessingService service;
    public SyncProcessingController(SyncProcessingService service){this.service=service;}
    public record ProcessRequest(UUID communityId, int limit){}
    public record ResolveRequest(UUID communityId,@NotBlank String decision){}
    public record RevokeRequest(UUID communityId,@NotBlank @Size(max=240) String reason){}
    @PostMapping("/process")
    public SyncProcessingService.ProcessResult process(@Valid @RequestBody ProcessRequest r, Authentication a){
        return service.process(user(a),r.communityId(),r.limit());
    }
    @GetMapping("/conflicts")
    public List<SyncProcessingService.ConflictView> conflicts(@RequestParam UUID communityId,Authentication a){
        return service.conflicts(user(a),communityId);
    }
    @PostMapping("/conflicts/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@PathVariable UUID id,@Valid @RequestBody ResolveRequest r,Authentication a){
        service.resolve(user(a),r.communityId(),id,r.decision());
    }
    @PostMapping("/devices/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id,@Valid @RequestBody RevokeRequest r,Authentication a){
        service.revokeDevice(user(a),r.communityId(),id,r.reason());
    }
    private UUID user(Authentication a){return UUID.fromString(a.getName());}
}
