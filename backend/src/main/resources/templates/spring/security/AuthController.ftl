package ${packageName}.controller;

import ${packageName}.dto.AuthRequest;
import ${packageName}.dto.AuthResponse;
import ${packageName}.dto.RegisterRequest;
import ${packageName}.config.JwtUtil;
<#if security.principalEntity??>
import ${packageName}.entity.${security.principalEntity};
import ${packageName}.repository.${security.principalEntity}Repository;
</#if>
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
<#if security.principalEntity??>
    private final ${security.principalEntity}Repository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            ${security.principalEntity}Repository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }
<#else>
    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }
</#if>

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        return ResponseEntity.ok(new AuthResponse(token, refreshToken, "Bearer"));
    }

<#if security.principalEntity??>
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findBy${security.usernameField?cap_first}(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Create new user
        ${security.principalEntity} user = new ${security.principalEntity}();
        user.set${security.usernameField?cap_first}(request.getUsername());
        user.set${security.passwordField?cap_first}(passwordEncoder.encode(request.getPassword()));
        
        userRepository.save(user);
        
        // Generate tokens - user implements UserDetails
        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        return ResponseEntity.ok(new AuthResponse(token, refreshToken, "Bearer"));
    }
</#if>

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody String refreshToken) {
        try {
            // Validate the refresh token first
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Remove quotes if present (from JSON body)
            refreshToken = refreshToken.replace("\"", "").trim();
            
            String username = jwtUtil.extractUsername(refreshToken);
            
            // Verify the token is not expired
            if (jwtUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(401).build();
            }
            
            // Generate new access token
            return ResponseEntity.ok(new AuthResponse(
                    jwtUtil.generateTokenFromUsername(username),
                    refreshToken,
                    "Bearer"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}
