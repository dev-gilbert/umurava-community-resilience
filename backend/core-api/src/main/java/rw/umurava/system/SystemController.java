package rw.umurava.system;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.Map;
@RestController @RequestMapping("/api/v1/system")
public class SystemController {
 @GetMapping("/info") public Map<String,Object> info() { return Map.of("service","UMURAVA Core API","apiVersion","v1","status","FOUNDATION","serverTime",Instant.now().toString()); }
}
