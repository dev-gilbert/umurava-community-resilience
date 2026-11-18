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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public AuthController(AppUserRepository users, RefreshSessionRepository sessions,
                          PasswordEncoder passwords, JwtService jwt, AuditService audit) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.jwt = jwt;
        this.audit = audit;
    }

    public record RegisterRequest(@NotBlank @Size(max = 180) String fullName,
                                  @NotBlank @Email @Size(max = 180) String email,
                                  @Size(max = 30) String phone,
                                  @NotBlank @Size(min = 12, max = 128) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account cannot be created with these details");
        }
        AppUser user = users.save(new AppUser(request.fullName().trim(), email, request.phone(),
            passwords.encode(request.password())));
        audit.record(user.getId(), "AUTH_REGISTER", "USER", user.getId().toString(), "SUCCESS");
        return issuePair(user, UUID.randomUUID());
    }

    @PostMapping("/login")
    @Transactional
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwords.matches(request.password(), user.getPasswordHash()) || !"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        audit.record(user.getId(), "AUTH_LOGIN", "USER", user.getId().toString(), "SUCCESS");
        return issuePair(user, UUID.randomUUID());
    }

    @PostMapping("/refresh")
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Map<String, Object> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshSession old = sessions.findByTokenHashForUpdate(hash(request.refreshToken()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!old.isActive()) {
            sessions.revokeActiveByFamilyId(old.getFamilyId());
            audit.record(old.getUserId(), "AUTH_REFRESH_REUSE_DETECTED", "SESSION_FAMILY",
                old.getFamilyId().toString(), "BLOCKED");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Refresh token is expired or has already been used; sign in again");
        }

        AppUser user = users.findById(old.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account unavailable"));

        String replacementToken = randomRefreshToken();
        RefreshSession replacement = sessions.save(new RefreshSession(user.getId(), hash(replacementToken),
            Instant.now().plus(30, ChronoUnit.DAYS), old.getFamilyId()));
        old.revoke(replacement.getId());
        sessions.save(old);

        audit.record(user.getId(), "AUTH_REFRESH", "SESSION", old.getId().toString(), "SUCCESS");
        return tokenResponse(user, replacementToken);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void logout(@Valid @RequestBody RefreshRequest request) {
        sessions.findByTokenHashForUpdate(hash(request.refreshToken())).ifPresent(session -> {
            if (session.isActive()) {
                session.revoke();
                audit.record(session.getUserId(), "AUTH_LOGOUT", "SESSION", session.getId().toString(), "SUCCESS");
            }
        });
    }

    @GetMapping("/me")
    public Map<String, Object> me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        UUID id = UUID.fromString(authentication.getName());
        AppUser user = users.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return Map.of("id", user.getId(), "fullName", user.getFullName(), "email", user.getEmail(),
            "phone", user.getPhone() == null ? "" : user.getPhone());
    }

    private Map<String, Object> issuePair(AppUser user, UUID familyId) {
        String refreshToken = randomRefreshToken();
        sessions.save(new RefreshSession(user.getId(), hash(refreshToken),
            Instant.now().plus(30, ChronoUnit.DAYS), familyId));
        return tokenResponse(user, refreshToken);
    }

    private Map<String, Object> tokenResponse(AppUser user, String refreshToken) {
        return Map.of("accessToken", jwt.issue(user), "tokenType", "Bearer", "expiresInSeconds", 600,
            "refreshToken", refreshToken,
            "user", Map.of("id", user.getId(), "fullName", user.getFullName(), "email", user.getEmail()));
    }

    private String randomRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
