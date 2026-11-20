package rw.umurava.emergency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/communities/{communityId}")
public class EmergencyController {
 private final EmergencyService service; public EmergencyController(EmergencyService service){this.service=service;}
 public record IncidentRequest(@NotBlank @Size(max=80)String incidentType,@Size(max=2000)String description,@NotBlank String severity,@DecimalMin("-90.0") @DecimalMax("90.0")Double latitude,@DecimalMin("-180.0") @DecimalMax("180.0")Double longitude){}
 public record IncidentUpdate(@NotBlank String status,@Size(max=1000)String note){}
 public record AnnouncementRequest(@NotBlank @Size(max=180)String title,@NotBlank @Size(max=3000)String message,@NotBlank String priority,Instant expiresAt){}
 @GetMapping("/incidents") public List<Map<String,Object>> incidents(@PathVariable UUID communityId,Authentication a){return service.incidents(uid(a),communityId);}
 @PostMapping("/incidents") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> report(@PathVariable UUID communityId,@Valid @RequestBody IncidentRequest r,Authentication a){return service.report(uid(a),communityId,r.incidentType(),r.description(),r.severity(),r.latitude(),r.longitude());}
 @PatchMapping("/incidents/{incidentId}/status") public Map<String,Object> update(@PathVariable UUID communityId,@PathVariable UUID incidentId,@Valid @RequestBody IncidentUpdate r,Authentication a){return service.updateIncident(uid(a),communityId,incidentId,r.status(),r.note());}
 @GetMapping("/announcements") public List<Map<String,Object>> announcements(@PathVariable UUID communityId,Authentication a){return service.announcements(uid(a),communityId);}
 @PostMapping("/announcements") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> publish(@PathVariable UUID communityId,@Valid @RequestBody AnnouncementRequest r,Authentication a){return service.publish(uid(a),communityId,r.title(),r.message(),r.priority(),r.expiresAt());}
 @PostMapping("/announcements/{announcementId}/acknowledgements") public Map<String,Integer> acknowledge(@PathVariable UUID communityId,@PathVariable UUID announcementId,Authentication a){return Map.of("acknowledged",service.acknowledge(uid(a),communityId,announcementId));}
 private UUID uid(Authentication a){return UUID.fromString(a.getName());}
}
