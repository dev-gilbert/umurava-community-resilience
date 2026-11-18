package rw.umurava.identity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="audit_events")
public class AuditEvent {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="actor_user_id") private UUID actorUserId;
 @Column(nullable=false,length=80) private String action;
 @Column(name="target_type",nullable=false,length=80) private String targetType;
 @Column(name="target_id",length=120) private String targetId;
 @Column(nullable=false,length=30) private String outcome;
 @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt;
 protected AuditEvent(){}
 public AuditEvent(UUID actor,String action,String targetType,String targetId,String outcome){this.actorUserId=actor;this.action=action;this.targetType=targetType;this.targetId=targetId;this.outcome=outcome;this.occurredAt=Instant.now();}
}
