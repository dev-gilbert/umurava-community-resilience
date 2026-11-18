package rw.umurava.identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final AppUserRepository users; private final RefreshSessionRepository sessions; private final PasswordEncoder passwords; private final JwtService jwt; private final AuditService audit; private final SecureRandom random=new SecureRandom();
 public AuthController(AppUserRepository users,RefreshSessionRepository sessions,PasswordEncoder passwords,JwtService jwt,AuditService audit){this.users=users;this.sessions=sessions;this.passwords=passwords;this.jwt=jwt;this.audit=audit;}
 public record RegisterRequest(@NotBlank @Size(max=180) String fullName,@NotBlank @Email @Size(max=180) String email,@Size(max=30) String phone,@NotBlank @Size(min=12,max=128) String password){}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
 public record RefreshRequest(@NotBlank String refreshToken){}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public Map<String,Object> register(@Valid @RequestBody RegisterRequest r){String email=r.email().trim().toLowerCase();if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"Account cannot be created with these details");AppUser user=users.save(new AppUser(r.fullName().trim(),email,r.phone(),passwords.encode(r.password())));audit.record(user.getId(),"AUTH_REGISTER","USER",user.getId().toString(),"SUCCESS");return issuePair(user);}
 @PostMapping("/login") @Transactional
 public Map<String,Object> login(@Valid @RequestBody LoginRequest r){AppUser user=users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password"));if(!passwords.matches(r.password(),user.getPasswordHash())||!"ACTIVE".equals(user.getStatus()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password");audit.record(user.getId(),"AUTH_LOGIN","USER",user.getId().toString(),"SUCCESS");return issuePair(user);}
 @PostMapping("/refresh") @Transactional
 public Map<String,Object> refresh(@Valid @RequestBody RefreshRequest r){String hash=hash(r.refreshToken());RefreshSession old=sessions.findByTokenHash(hash).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid refresh token"));if(!old.isActive()){old.revoke();throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Refresh token expired or revoked");}old.revoke();AppUser user=users.findById(old.getUserId()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account unavailable"));audit.record(user.getId(),"AUTH_REFRESH","SESSION",old.getId().toString(),"SUCCESS");return issuePair(user);}
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 public void logout(@Valid @RequestBody RefreshRequest r){sessions.findByTokenHash(hash(r.refreshToken())).ifPresent(s->{s.revoke();audit.record(s.getUserId(),"AUTH_LOGOUT","SESSION",s.getId().toString(),"SUCCESS");});}
 @GetMapping("/me") public Map<String,Object> me(org.springframework.security.core.Authentication auth){if(auth==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);UUID id=UUID.fromString(auth.getName());AppUser u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));return Map.of("id",u.getId(),"fullName",u.getFullName(),"email",u.getEmail(),"phone",u.getPhone()==null?"":u.getPhone());}
 private Map<String,Object> issuePair(AppUser u){byte[] bytes=new byte[48];random.nextBytes(bytes);String refresh=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);sessions.save(new RefreshSession(u.getId(),hash(refresh),Instant.now().plus(30,ChronoUnit.DAYS)));return Map.of("accessToken",jwt.issue(u),"tokenType","Bearer","expiresInSeconds",600,"refreshToken",refresh,"user",Map.of("id",u.getId(),"fullName",u.getFullName(),"email",u.getEmail()));}
 private String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
