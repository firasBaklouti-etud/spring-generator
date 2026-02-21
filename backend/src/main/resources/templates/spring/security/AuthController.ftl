package ${packageName}.controller;

import ${packageName}.dto.AuthRequest;
import ${packageName}.dto.AuthResponse;
<#assign hasRegistration = !(security.registrationEnabled?? && !security.registrationEnabled)>
<#if hasRegistration>
import ${packageName}.dto.RegisterRequest;
</#if>
import ${packageName}.config.JwtUtil;
<#if security.principalEntity??>
import ${packageName}.entity.${security.principalEntity};
import ${packageName}.repository.${security.principalEntity}Repository;
</#if>
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
import ${packageName}.entity.Role;
import ${packageName}.repository.RoleRepository;
</#if>
<#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>
import ${packageName}.security.RefreshToken;
import ${packageName}.security.RefreshTokenService;
</#if>
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
<#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>
import org.springframework.security.core.userdetails.UserDetailsService;
</#if>
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
<#if security.principalEntity??>
    private final ${security.principalEntity}Repository userRepository;
</#if>
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
    private final RoleRepository roleRepository;
</#if>
<#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
</#if>

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder<#if security.principalEntity??>,
            ${security.principalEntity}Repository userRepository</#if><#if security.rbacMode?? && security.rbacMode == "DYNAMIC">,
            RoleRepository roleRepository</#if><#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>,
            RefreshTokenService refreshTokenService,
            UserDetailsService userDetailsService</#if>) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
<#if security.principalEntity??>
        this.userRepository = userRepository;
</#if>
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
        this.roleRepository = roleRepository;
</#if>
<#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
</#if>
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

<#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>
        // Create DB-persisted refresh token
        ${security.principalEntity} user = userRepository.findBy${security.usernameField?cap_first}(request.getUsername())
                .orElseThrow();
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user);
        String refreshToken = refreshTokenEntity.getToken();
<#else>
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
</#if>

        // Extract roles and permissions from authorities
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toList());
        Set<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        return ResponseEntity.ok(new AuthResponse(token, refreshToken, "Bearer", roles, permissions));
    }

<#if hasRegistration && security.principalEntity??>
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
<#if security.usernameField != "email">
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
</#if>

<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
        // Assign default USER role
        Role defaultRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER")));
        user.setRoles(List.of(defaultRole));
</#if>

        userRepository.save(user);

        // Generate tokens - user implements UserDetails
        String token = jwtUtil.generateToken(user);
<#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user);
        String refreshToken = refreshTokenEntity.getToken();
<#else>
        String refreshToken = jwtUtil.generateRefreshToken(user);
</#if>

        // Extract roles and permissions
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toList());
        Set<String> permissions = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        return ResponseEntity.ok(new AuthResponse(token, refreshToken, "Bearer", roles, permissions));
    }
</#if>

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Refresh token is required"));
            }

<#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>
            // Verify DB-persisted refresh token
            RefreshToken storedToken = refreshTokenService.verifyRefreshToken(refreshToken)
                    .orElse(null);
            if (storedToken == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "Invalid or expired refresh token"));
            }

            // Rotate the refresh token
            RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(storedToken);

            // Generate new access token
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                    storedToken.getUser().get${security.usernameField?cap_first}());
            String newAccessToken = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(new AuthResponse(
                    newAccessToken,
                    newRefreshToken.getToken(),
                    "Bearer"
            ));
<#else>
            String username = jwtUtil.extractUsername(refreshToken);

            // Verify the token is not expired
            if (jwtUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "Refresh token has expired"));
            }

            // Generate new access token
            return ResponseEntity.ok(new AuthResponse(
                    jwtUtil.generateTokenFromUsername(username),
                    refreshToken,
                    "Bearer"
            ));
</#if>
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid refresh token"));
        }
    }

<#if security.refreshTokenPersisted?? && security.refreshTokenPersisted>
    /**
     * Logout endpoint - revokes all refresh tokens for the authenticated user.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            if (refreshToken != null) {
                refreshTokenService.verifyRefreshToken(refreshToken)
                        .ifPresent(rt -> refreshTokenService.revokeAllUserTokens(rt.getUser()));
            }
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Logged out"));
        }
    }
</#if>
}
