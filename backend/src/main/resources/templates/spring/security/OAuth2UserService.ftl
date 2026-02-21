package ${packageName}.security;

import ${packageName}.entity.${principalEntity};
import ${packageName}.repository.${principalEntity}Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom OAuth2 user service that maps social login profiles to local user entities.
 * Supports Google, GitHub, and Facebook providers.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final ${principalEntity}Repository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(${principalEntity}Repository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(registrationId, attributes);
        String name = extractName(registrationId, attributes);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not available from " + registrationId);
        }

        // Find or create the local user
        Optional<${principalEntity}> existingUser = userRepository.findBy${usernameField?cap_first}(email);
        if (existingUser.isEmpty()) {
            ${principalEntity} newUser = new ${principalEntity}();
            newUser.set${usernameField?cap_first}(email);
            // Set a random password for OAuth2 users (they won't use password login)
            newUser.set${passwordField?cap_first}(passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.save(newUser);
            log.info("Created new user from {} OAuth2 login: {}", registrationId, email);
        }

        return oAuth2User;
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> (String) attributes.get("email");
            case "github" -> (String) attributes.get("email");
            case "facebook" -> (String) attributes.get("email");
            default -> null;
        };
    }

    private String extractName(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> (String) attributes.get("name");
            case "github" -> (String) attributes.get("login");
            case "facebook" -> (String) attributes.get("name");
            default -> null;
        };
    }
}
