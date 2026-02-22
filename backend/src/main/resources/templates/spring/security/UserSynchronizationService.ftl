package ${packageName}.service;

import ${packageName}.entity.${security.principalEntity};
import ${packageName}.repository.${security.principalEntity}Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service that synchronizes Keycloak users to the local database.
 * After OIDC login, ensures a local user entity exists by creating or
 * updating it from the OIDC token claims.
 */
@Service
public class UserSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(UserSynchronizationService.class);

    private final ${security.principalEntity}Repository userRepository;

    public UserSynchronizationService(${security.principalEntity}Repository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Synchronizes an OIDC user to the local database.
     * Creates a new local user if one does not exist, or updates the existing record.
     *
     * @param oidcUser the authenticated OIDC user from Keycloak
     * @return the local user entity
     */
    @Transactional
    public ${security.principalEntity} synchronize(OidcUser oidcUser) {
        String username = oidcUser.getPreferredUsername();
        String email = oidcUser.getEmail();
        String fullName = oidcUser.getFullName();

        if (username == null || username.isBlank()) {
            username = email;
        }

        Optional<${security.principalEntity}> existingUser = userRepository.findBy${security.usernameField?cap_first}(username);

        if (existingUser.isPresent()) {
            ${security.principalEntity} user = existingUser.get();
            log.debug("Updating existing local user from Keycloak: {}", username);
            updateUserFromClaims(user, email, fullName);
            return userRepository.save(user);
        } else {
            log.info("Creating new local user from Keycloak OIDC login: {}", username);
            ${security.principalEntity} newUser = createUserFromClaims(username, email, fullName);
            return userRepository.save(newUser);
        }
    }

    private ${security.principalEntity} createUserFromClaims(String username, String email, String fullName) {
        ${security.principalEntity} user = new ${security.principalEntity}();
        user.set${security.usernameField?cap_first}(username);
        // Password is not needed for OIDC-authenticated users
        user.set${security.passwordField?cap_first}("OIDC_MANAGED");
        return user;
    }

    private void updateUserFromClaims(${security.principalEntity} user, String email, String fullName) {
        if (email != null) {
            user.set${security.usernameField?cap_first}(email);
        }
    }
}
