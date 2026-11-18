package rw.umurava.identity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
@Service
public class JwtService {
 private final SecretKey key; private final String issuer; private final long ttlMinutes;
 public JwtService(@Value("${umurava.jwt.secret}") String secret,@Value("${umurava.jwt.issuer:umurava-local}") String issuer,@Value("${umurava.jwt.expiration-minutes:10}") long ttlMinutes){if(secret==null||secret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalStateException("UMURAVA_JWT_SECRET must contain at least 32 bytes");this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));this.issuer=issuer;this.ttlMinutes=ttlMinutes;}
 public String issue(AppUser user){Instant now=Instant.now();return Jwts.builder().issuer(issuer).subject(user.getId().toString()).id(UUID.randomUUID().toString()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttlMinutes*60))).signWith(key).compact();}
 public UUID validateAndGetUserId(String token){return UUID.fromString(Jwts.parser().verifyWith(key).requireIssuer(issuer).build().parseSignedClaims(token).getPayload().getSubject());}
}
