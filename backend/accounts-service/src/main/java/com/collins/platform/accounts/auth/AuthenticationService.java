package com.collins.platform.accounts.auth;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.jdbc.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    public record LoginResult(String userId, String email, String name, String role, String tenantId,
                              String accessToken, String refreshToken, long expiresIn) {
    }

    public record UserProfile(String id, String email, String name, String role, String tenantId) {
    }

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepo, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfile register(String email, String password, String name, String role, String tenantId) {
        if (userRepo.existsByEmail(email)) {
            throw new BusinessRuleException("USER_ALREADY_EXISTS", "User with email " + email + " already exists");
        }

        if (password == null || password.length() < 8) {
            throw new BusinessRuleException("INVALID_PASSWORD", "Password must be at least 8 characters long");
        }

        String userId = Ids.newId("usr");
        String passwordHash = passwordEncoder.encode(password);

        UserRepository.User user = new UserRepository.User(userId, email, passwordHash, name, role, tenantId);
        userRepo.save(user);

        log.info("User registered: {} ({})", email, userId);

        return new UserProfile(userId, email, name, role, tenantId);
    }

    @Transactional(readOnly = true)
    public LoginResult login(String email, String password) {
        UserRepository.User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("INVALID_CREDENTIALS", "Invalid email or password"));

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.role(), user.tenantId());
        String refreshToken = jwtService.generateRefreshToken(user.id());
        long expiresIn = jwtService.getAccessTokenExpirationSeconds();

        log.info("User logged in: {} ({})", email, user.id());

        return new LoginResult(user.id(), user.email(), user.name(), user.role(), user.tenantId(),
                accessToken, refreshToken, expiresIn);
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(String userId) {
        UserRepository.User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "User not found"));

        return new UserProfile(user.id(), user.email(), user.name(), user.role(), user.tenantId());
    }
}
