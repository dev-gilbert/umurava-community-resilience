package rw.umurava.identity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class AuditService {
 private final AuditEventRepository events;
 public AuditService(AuditEventRepository events){this.events=events;}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void record(UUID actor,String action,String targetType,String targetId,String outcome){events.save(new AuditEvent(actor,action,targetType,targetId,outcome));}
}
