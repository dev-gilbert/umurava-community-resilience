package rw.umurava.identity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="app_users")
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="full_name",nullable=false,length=180) private String fullName;
 @Column(nullable=false,unique=true,length=180) private String email;
 @Column(length=30) private String phone;
 @Column(name="password_hash",nullable=false) private String passwordHash;
 @Column(nullable=false,length=30) private String status="ACTIVE";
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected AppUser(){}
 public AppUser(String fullName,String email,String phone,String passwordHash){this.fullName=fullName;this.email=email;this.phone=phone;this.passwordHash=passwordHash;this.status="ACTIVE";}
 @PrePersist void onCreate(){Instant now=Instant.now();createdAt=now;updatedAt=now;}
 @PreUpdate void onUpdate(){updatedAt=Instant.now();}
 public UUID getId(){return id;} public String getFullName(){return fullName;} public String getEmail(){return email;} public String getPhone(){return phone;} public String getPasswordHash(){return passwordHash;} public String getStatus(){return status;}
}
