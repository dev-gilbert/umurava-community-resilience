package rw.umurava.command;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/command")
public class RegionalDashboardController {
 private final RegionalDashboardService service; public RegionalDashboardController(RegionalDashboardService service){this.service=service;}
 @GetMapping("/districts/{districtName}/overview") public Map<String,Object> district(@PathVariable String districtName,Authentication auth){return service.district(uid(auth),districtName);}
 @GetMapping("/national/overview") public Map<String,Object> national(Authentication auth){return service.national(uid(auth));}
 private UUID uid(Authentication auth){return UUID.fromString(auth.getName());}
}
