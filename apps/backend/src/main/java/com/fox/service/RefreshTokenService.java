package com.fox.service;

import com.fox.entity.User;
import com.fox.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public int revokeActiveTokens(User user) {
        return refreshTokenRepository.revokeActiveTokensByUser(user);
    }
}
