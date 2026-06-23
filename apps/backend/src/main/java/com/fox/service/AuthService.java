package com.fox.service;

import com.fox.dto.AuthLoginRequest;
import com.fox.dto.AuthRegisterRequest;
import com.fox.dto.AuthResponse;
import com.fox.dto.ForgotPasswordRequest;
import com.fox.dto.RefreshTokenRequest;
import com.fox.dto.ResetPasswordRequest;
import com.fox.entity.LoginHistory;
import com.fox.entity.PasswordResetToken;
import com.fox.entity.RefreshToken;
import com.fox.entity.Role;
import com.fox.entity.User;
import com.fox.exception.EmailAlreadyExistsException;
import com.fox.exception.InvalidCredentialsException;
import com.fox.exception.InvalidTokenException;
import com.fox.exception.ResourceNotFoundException;
import com.fox.exception.TokenExpiredException;
import com.fox.mapper.UserMapper;
import com.fox.repository.LoginHistoryRepository;
import com.fox.repository.PasswordResetTokenRepository;
import com.fox.repository.RefreshTokenRepository;
import com.fox.repository.RoleRepository;
import com.fox.repository.UserRepository;
import com.fox.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final long refreshTokenExpiration;
    private final long passwordResetTokenExpiration;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginHistoryRepository loginHistoryRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            @Value("${refresh-token.expiration}") long refreshTokenExpiration,
            @Value("${password-reset-token.expiration}") long passwordResetTokenExpiration
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.passwordResetTokenExpiration = passwordResetTokenExpiration;
    }

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role USER not found"));

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setEmailVerified(false);
        user.getRoles().add(userRole);
        user = userRepository.save(user);

        RefreshToken refreshToken = createRefreshToken(user);
        return new AuthResponse(jwtService.generateToken(user), refreshToken.getToken(), UserMapper.toResponse(user));
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!user.isActive() || user.getDeletedAt() != null) {
            registerLoginHistory(user, httpRequest, false);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            registerLoginHistory(user, httpRequest, false);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);
        registerLoginHistory(user, httpRequest, true);

        RefreshToken refreshToken = createRefreshToken(user);
        return new AuthResponse(jwtService.generateToken(user), refreshToken.getToken(), UserMapper.toResponse(user));
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TokenExpiredException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        RefreshToken nextRefreshToken = createRefreshToken(user);
        return new AuthResponse(jwtService.generateToken(user), nextRefreshToken.getToken(), UserMapper.toResponse(user));
    }

    @Transactional
    public Map<String, String> logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken()).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
        return Map.of("message", "Logged out successfully");
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        userRepository.findByEmail(email)
                .filter(user -> user.isActive() && user.getDeletedAt() == null)
                .ifPresent(this::createPasswordResetToken);

        return Map.of("message", "If the email exists, a password reset token was generated");
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Invalid password reset token");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Password reset token expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenService.revokeActiveTokens(user);

        return Map.of("message", "Password updated successfully");
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plus(java.time.Duration.ofMillis(refreshTokenExpiration)));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    private void createPasswordResetToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plus(java.time.Duration.ofMillis(passwordResetTokenExpiration)));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);
    }

    private void registerLoginHistory(User user, HttpServletRequest request, boolean success) {
        LoginHistory history = new LoginHistory();
        history.setUser(user);
        history.setIpAddress(extractIp(request));
        history.setUserAgent(request.getHeader("User-Agent"));
        history.setSuccess(success);
        loginHistoryRepository.save(history);
    }

    private String extractIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
