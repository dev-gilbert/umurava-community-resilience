package rw.umurava.resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/communities/{communityId}/resources")
public class ResourceController {
 private final ResourceService service; public ResourceController(ResourceService service){this.service=service;}
 public record CreateRequest(@NotBlank @Size(max=150) String name,@NotBlank @Size(max=40) String category,@Size(max=2000) String description,@NotNull @DecimalMin("0.0") BigDecimal quantity,@NotNull @DecimalMin("0.0") BigDecimal minimumQuantity,@NotBlank @Size(max=30) String unit){}
 public record AdjustRequest(@NotBlank String direction,@NotNull @DecimalMin("0.001") BigDecimal quantity,@NotBlank @Size(max=120) String idempotencyKey){}
 @GetMapping public List<ResourceService.ResourceView> list(@PathVariable UUID communityId,@RequestParam(required=false)String category,Authentication auth){return service.list(uid(auth),communityId,category);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ResourceService.ResourceView create(@PathVariable UUID communityId,@Valid @RequestBody CreateRequest r,Authentication auth){return service.create(uid(auth),communityId,new ResourceService.CreateResource(r.name(),r.category(),r.description(),r.quantity(),r.minimumQuantity(),r.unit()));}
 @PostMapping("/{resourceId}/adjustments") public ResourceService.QuantityResult adjust(@PathVariable UUID communityId,@PathVariable UUID resourceId,@Valid @RequestBody AdjustRequest r,Authentication auth){return service.adjust(uid(auth),communityId,resourceId,r.direction(),r.quantity(),r.idempotencyKey());}
 private UUID uid(Authentication auth){return UUID.fromString(auth.getName());}
}
