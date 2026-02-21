package ${packageName}.security;

import ${packageName}.config.JwtUtil;
import ${packageName}.entity.${principalEntity};
import ${packageName}.repository.${principalEntity}Repository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling social login code exchange.
 * Receives the OAuth2 authorization code from the frontend and exchanges it for a JWT.
 */
@RestController
@RequestMapping("/api/auth/social")
public class SocialAuthController {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final ${principalEntity}Repository userRepository;

    public SocialAuthController(JwtUtil jwtUtil,
                                 UserDetailsService userDetailsService,
                                 ${principalEntity}Repository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    /**
     * After successful OAuth2 login, issue a JWT for the authenticated user.
     * The frontend sends the user's email (obtained from the OAuth2 provider callback).
     */
    @PostMapping("/token")
    public ResponseEntity<?> exchangeForToken(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required"));
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "tokenType", "Bearer"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "User not found. Complete OAuth2 login first."));
        }
    }
}
