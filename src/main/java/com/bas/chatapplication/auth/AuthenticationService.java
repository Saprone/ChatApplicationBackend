package com.bas.chatapplication.auth;

import com.bas.chatapplication.config.JwtService;
import com.bas.chatapplication.mfa.TwoFactorAuthenticationService;
import com.bas.chatapplication.token.Token;
import com.bas.chatapplication.token.TokenRepository;
import com.bas.chatapplication.token.TokenType;
import com.bas.chatapplication.user.Role;
import com.bas.chatapplication.user.User;
import com.bas.chatapplication.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TwoFactorAuthenticationService twoFactorAuthenticationService;

    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role((request.getRole() == null) ? Role.USER : request.getRole())
                .mfaEnabled(request.isMfaEnabled())
                .build();

        if (request.isMfaEnabled()) {
            user.setSecret(twoFactorAuthenticationService.generateNewSecret());
        }

        var savedUser = repository.save(user);
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        saveUserToken(savedUser, accessToken);

        return AuthenticationResponse.builder()
            .secretImageUri(twoFactorAuthenticationService.generateQrCodeImageUri(user.getSecret()))
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .mfaEnabled(user.isMfaEnabled())
            .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        var user = repository.findByEmail(request.getEmail()).orElseThrow();

        if (user.isMfaEnabled()) {
            return AuthenticationResponse.builder()
                .accessToken("")
                .refreshToken("")
                .mfaEnabled(true)
                .build();
        }

        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .mfaEnabled(false)
            .build();
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader("Authorization");
        final String refreshToken;
        final String userEmail;

        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
            return;
        }

        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail).orElseThrow();

            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .mfaEnabled(false)
                    .build();

                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

    private void saveUserToken(User user, String accessToken) {
        var token = Token.builder()
                .user(user)
                .token(accessToken)
                .tokenType(TokenType.BEARER)
                .revoked(false)
                .expired(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokensByUser(user.getId());

        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

    public AuthenticationResponse verifyCode(VerificationRequest verificationRequest) {
        User user = repository
            .findByEmail(verificationRequest.getEmail())
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("No user found with %S", verificationRequest.getEmail()))
            );

        if (twoFactorAuthenticationService.isOtpNotValid(user.getSecret(), verificationRequest.getCode())) {
            throw new BadCredentialsException("Code is not correct");
        }

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
            .accessToken(jwtToken)
            .mfaEnabled(user.isMfaEnabled())
            .build();
    }
}
