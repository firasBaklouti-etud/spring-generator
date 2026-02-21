package ${packageName}.security;

import ${packageName}.entity.${principalEntity};
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing database-persisted refresh tokens.
 * Supports token rotation and automatic cleanup of expired tokens.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${"$"}{jwt.refresh-expiration:604800000}")
    private long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Create a new refresh token for the given user.
     * Revokes any existing tokens for the user first.
     */
    @Transactional
    public RefreshToken createRefreshToken(${principalEntity} user) {
        // Revoke existing tokens
        refreshTokenRepository.revokeAllByUserId(user.getId());

        LocalDateTime expiryDate = LocalDateTime.now().plusNanos(refreshTokenDurationMs * 1_000_000);
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                user,
                expiryDate
        );

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify the refresh token is valid (exists, not expired, not revoked).
     */
    public Optional<RefreshToken> verifyRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> !rt.isRevoked())
                .filter(rt -> !rt.isExpired());
    }

    /**
     * Rotate the refresh token: revoke the old one, create a new one.
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        LocalDateTime expiryDate = LocalDateTime.now().plusNanos(refreshTokenDurationMs * 1_000_000);
        RefreshToken newToken = new RefreshToken(
                UUID.randomUUID().toString(),
                oldToken.getUser(),
                expiryDate
        );

        return refreshTokenRepository.save(newToken);
    }

    /**
     * Revoke all refresh tokens for a user (e.g., on logout).
     */
    @Transactional
    public void revokeAllUserTokens(${principalEntity} user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    /**
     * Cleanup expired tokens. Runs every hour.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
