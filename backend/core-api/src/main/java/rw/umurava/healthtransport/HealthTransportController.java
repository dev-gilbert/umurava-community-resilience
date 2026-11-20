package rw.umurava.healthtransport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/communities/{communityId}")
public class HealthTransportController {
 private final HealthTransportService service; public HealthTransportController(HealthTransportService service){this.service=service;}
 public record FacilityRequest(@NotBlank @Size(max=160)String name,@NotBlank @Size(max=40)String facilityType){}
 public record FacilityStatusRequest(@NotBlank String serviceStatus,@Min(0)int bedsAvailable,boolean oxygenAvailable){}
 public record VehicleRequest(@NotBlank @Size(max=40)String vehicleCode,@NotBlank @Size(max=40)String vehicleType,@Min(1)int capacity,Double latitude,Double longitude){}
 public record TransportRequest(@NotBlank @Size(max=40)String requestType,@NotBlank String priority,@DecimalMin("-90.0") @DecimalMax("90.0")double pickupLatitude,@DecimalMin("-180.0") @DecimalMax("180.0")double pickupLongitude,@DecimalMin("-90.0") @DecimalMax("90.0")double destinationLatitude,@DecimalMin("-180.0") @DecimalMax("180.0")double destinationLongitude,@Size(max=500)String notes){}
 public record TransportUpdate(@NotBlank String status,UUID vehicleId){}
 @GetMapping("/health-facilities") public List<Map<String,Object>> facilities(@PathVariable UUID communityId,Authentication a){return service.facilities(uid(a),communityId);}
 @PostMapping("/health-facilities") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> addFacility(@PathVariable UUID communityId,@Valid @RequestBody FacilityRequest r,Authentication a){return service.addFacility(uid(a),communityId,r.name(),r.facilityType());}
 @PatchMapping("/health-facilities/{facilityId}/availability") public Map<String,Object> updateFacility(@PathVariable UUID communityId,@PathVariable UUID facilityId,@Valid @RequestBody FacilityStatusRequest r,Authentication a){return service.updateFacility(uid(a),communityId,facilityId,r.serviceStatus(),r.bedsAvailable(),r.oxygenAvailable());}
 @GetMapping("/transport-vehicles") public List<Map<String,Object>> vehicles(@PathVariable UUID communityId,Authentication a){return service.vehicles(uid(a),communityId);}
 @PostMapping("/transport-vehicles") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> addVehicle(@PathVariable UUID communityId,@Valid @RequestBody VehicleRequest r,Authentication a){return service.addVehicle(uid(a),communityId,r.vehicleCode(),r.vehicleType(),r.capacity(),r.latitude(),r.longitude());}
 @GetMapping("/transport-requests") public List<Map<String,Object>> requests(@PathVariable UUID communityId,Authentication a){return service.requests(uid(a),communityId);}
 @PostMapping("/transport-requests") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createRequest(@PathVariable UUID communityId,@Valid @RequestBody TransportRequest r,Authentication a){return service.createRequest(uid(a),communityId,r.requestType(),r.priority(),r.pickupLatitude(),r.pickupLongitude(),r.destinationLatitude(),r.destinationLongitude(),r.notes());}
 @PatchMapping("/transport-requests/{requestId}/status") public Map<String,Object> updateRequest(@PathVariable UUID communityId,@PathVariable UUID requestId,@Valid @RequestBody TransportUpdate r,Authentication a){return service.updateRequest(uid(a),communityId,requestId,r.status(),r.vehicleId());}
 private UUID uid(Authentication a){return UUID.fromString(a.getName());}
}
