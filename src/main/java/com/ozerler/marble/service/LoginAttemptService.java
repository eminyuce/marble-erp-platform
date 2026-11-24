package com.ozerler.marble.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ozerler.marble.config.TwoFactorProperties;
import com.ozerler.marble.model.User;
import com.ozerler.marble.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Google-free brute-force protection.
 * <ul>
 *     <li>Per client IP: at most N failed logins (password or code) inside a sliding window (Caffeine).</li>
 *     <li>Per account: N consecutive failures lock the account for a fixed time (database, all replicas).</li>
 * </ul>
 * Every failure is logged with user id, client IP and time. Passwords and codes are never logged.
 */
@Service
@Slf4j
public class LoginAttemptService {

    private static final long MAX_TRACKED_IPS = 100_000L;

    private final UserRepository userRepository;
    private final TwoFactorProperties properties;
    private final Cache<String, Deque<Long>> ipFailures;
    private Clock clock = Clock.systemDefaultZone();

    public LoginAttemptService(UserRepository userRepository, TwoFactorProperties properties) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.ipFailures = Caffeine.newBuilder()
                .expireAfterAccess(properties.getIpWindow())
                .maximumSize(MAX_TRACKED_IPS)
                .build();
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Whether the client IP has used up its failed-login budget.
     *
     * @param clientIp resolved client address
     * @return true when the next attempt must be refused
     */
    public boolean isIpBlocked(String clientIp) {
        Deque<Long> failures = ipFailures.getIfPresent(key(clientIp));
        if (failures == null) {
            return false;
        }
        prune(failures);
        return failures.size() >= properties.getIpMaxFailures();
    }

    /**
     * Counts one failed attempt for the client IP.
     *
     * @param clientIp resolved client address
     */
    public void recordIpFailure(String clientIp) {
        Deque<Long> failures = ipFailures.get(key(clientIp), k -> new ConcurrentLinkedDeque<>());
        failures.addLast(clock.millis());
        prune(failures);
    }

    /**
     * Records a wrong password. Unknown user names only count against the IP.
     *
     * @param username e-mail or user name as typed
     * @param clientIp resolved client address
     * @return LOCKED when this failure locked the account
     */
    @Transactional
    public Outcome recordPasswordFailure(String username, String clientIp) {
        recordIpFailure(clientIp);
        Optional<User> user = findUser(username);
        if (user.isEmpty()) {
            log.warn("Failed login attempt: userId=unknown ip={} time={}", clientIp, LocalDateTime.now(clock));
            return Outcome.FAILED;
        }
        return countAccountFailure(user.get().getId(), clientIp, "password");
    }

    /**
     * Records a wrong TOTP or backup code.
     *
     * @param userId the pending user
     * @param clientIp resolved client address
     * @return LOCKED when this failure locked the account
     */
    @Transactional
    public Outcome recordSecondFactorFailure(Long userId, String clientIp) {
        recordIpFailure(clientIp);
        return countAccountFailure(userId, clientIp, "second-factor");
    }

    /**
     * Clears the account counter and lock after a full login.
     *
     * @param userId user id
     */
    @Transactional
    public void resetAccount(Long userId) {
        userRepository.resetLoginFailures(userId);
    }

    /**
     * Remaining attempts before the account locks.
     *
     * @param userId user id
     * @return attempts left, never negative
     */
    @Transactional(readOnly = true)
    public int remainingAttempts(Long userId) {
        Integer used = userRepository.findFailedLoginAttempts(userId);
        int count = 0;
        if (used != null) {
            count = used;
        }
        return Math.max(0, properties.getAccountMaxFailures() - count);
    }

    private Outcome countAccountFailure(Long userId, String clientIp, String factor) {
        userRepository.incrementFailedLoginAttempts(userId);
        Integer count = userRepository.findFailedLoginAttempts(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        log.warn("Failed login attempt: userId={} ip={} time={} factor={} consecutive={}",
                userId, clientIp, now, factor, count);
        if (count != null && count >= properties.getAccountMaxFailures()) {
            LocalDateTime until = now.plus(properties.getAccountLockDuration());
            userRepository.lockAccount(userId, until);
            log.warn("SECURITY_AUDIT account locked: userId={} ip={} until={} after {} failed attempts",
                    userId, clientIp, until, count);
            return Outcome.LOCKED;
        }
        return Outcome.FAILED;
    }

    private Optional<User> findUser(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String trimmed = username.trim();
        return userRepository.findByEmail(trimmed).or(() -> userRepository.findByUsername(trimmed))
                .filter(u -> !u.isDeleted());
    }

    private void prune(Deque<Long> failures) {
        long cutoff = clock.millis() - properties.getIpWindow().toMillis();
        while (!failures.isEmpty() && failures.peekFirst() < cutoff) {
            failures.pollFirst();
        }
    }

    private static String key(String clientIp) {
        if (clientIp == null) {
            return "";
        }
        return clientIp;
    }

    /** Result of a recorded failure. */
    public enum Outcome {
        FAILED, LOCKED
    }
}
