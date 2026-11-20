package rw.umurava.essentials;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/communities/{communityId}")
public class EssentialServicesController {
 private final EssentialServicesService service; public EssentialServicesController(EssentialServicesService service){this.service=service;}
 public record FoodRequest(@NotBlank @Size(max=160)String name,@NotBlank @Size(max=60)String category,@NotNull @DecimalMin("0.0")BigDecimal quantity,@NotBlank @Size(max=30)String unit,LocalDate expiryDate,@Size(max=180)String storageLocation){}
 public record WaterRequest(@NotBlank @Size(max=160)String name,@NotBlank @Size(max=40)String sourceType,@DecimalMin("0.0")BigDecimal estimatedLiters,Double latitude,Double longitude){}
 public record WaterTestRequest(@DecimalMin("0.0")BigDecimal phValue,@DecimalMin("0.0")BigDecimal turbidityNtu,Boolean ecoliDetected,@Size(max=2000)String notes){}
 public record EnergyRequest(@NotBlank @Size(max=160)String name,@NotBlank @Size(max=40)String assetType,@DecimalMin("0.0")BigDecimal capacityKwh,@DecimalMin("0.0")BigDecimal currentOutputKw,@NotBlank String operationalStatus,@Size(max=180)String locationDescription){}
 @GetMapping("/food-batches") public List<Map<String,Object>> food(@PathVariable UUID communityId,Authentication a){return service.food(uid(a),communityId);}
 @PostMapping("/food-batches") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> addFood(@PathVariable UUID communityId,@Valid @RequestBody FoodRequest r,Authentication a){return service.addFood(uid(a),communityId,r.name(),r.category(),r.quantity(),r.unit(),r.expiryDate(),r.storageLocation());}
 @GetMapping("/water-sources") public List<Map<String,Object>> water(@PathVariable UUID communityId,Authentication a){return service.water(uid(a),communityId);}
 @PostMapping("/water-sources") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> addWater(@PathVariable UUID communityId,@Valid @RequestBody WaterRequest r,Authentication a){return service.addWater(uid(a),communityId,r.name(),r.sourceType(),r.estimatedLiters(),r.latitude(),r.longitude());}
 @PostMapping("/water-sources/{sourceId}/quality-tests") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> addTest(@PathVariable UUID communityId,@PathVariable UUID sourceId,@Valid @RequestBody WaterTestRequest r,Authentication a){return service.addWaterTest(uid(a),communityId,sourceId,r.phValue(),r.turbidityNtu(),r.ecoliDetected(),r.notes());}
 @GetMapping("/water-sources/{sourceId}/quality-tests") public List<Map<String,Object>> tests(@PathVariable UUID communityId,@PathVariable UUID sourceId,Authentication a){return service.tests(uid(a),communityId,sourceId);}
 @GetMapping("/energy-assets") public List<Map<String,Object>> energy(@PathVariable UUID communityId,Authentication a){return service.energy(uid(a),communityId);}
 @PostMapping("/energy-assets") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> addEnergy(@PathVariable UUID communityId,@Valid @RequestBody EnergyRequest r,Authentication a){return service.addEnergy(uid(a),communityId,r.name(),r.assetType(),r.capacityKwh(),r.currentOutputKw(),r.operationalStatus(),r.locationDescription());}
 private UUID uid(Authentication a){return UUID.fromString(a.getName());}
}
