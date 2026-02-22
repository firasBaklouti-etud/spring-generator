package ${packageName};

import ${packageName}.dto.AuthRequest;
import ${packageName}.dto.AuthResponse;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base integration test class providing authenticated HTTP helpers.
 *
 * <p>Subclasses inherit a running server on a random port, a
 * {@link TestRestTemplate}, and convenience methods that obtain
 * Bearer tokens through the {@code /api/auth/login} endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIT {

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Authenticates against {@code /api/auth/login} and returns
     * {@link HttpHeaders} containing the Bearer token.
     *
     * @param username the username credential
     * @param password the password credential
     * @return headers with {@code Authorization: Bearer <token>}
     */
    protected HttpHeaders getAuthHeaders(String username, String password) {
        AuthRequest request = new AuthRequest(username, password);

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AuthRequest> entity = new HttpEntity<>(request, loginHeaders);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", entity, AuthResponse.class);

        String token = response.getBody() != null ? response.getBody().getAccessToken() : "";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    /**
     * Returns {@link HttpHeaders} authenticated with the default admin account.
     */
    protected HttpHeaders getAdminHeaders() {
        return getAuthHeaders("admin@admin.com", "admin123");
    }

    /**
     * Returns {@link HttpHeaders} authenticated with the default user account.
     */
    protected HttpHeaders getUserHeaders() {
        return getAuthHeaders("user@test.com", "user123");
    }
}
