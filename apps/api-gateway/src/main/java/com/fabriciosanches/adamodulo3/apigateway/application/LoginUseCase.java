package com.fabriciosanches.adamodulo3.apigateway.application;

import com.fabriciosanches.adamodulo3.apigateway.adapter.out.security.AuthUserEntity;
import com.fabriciosanches.adamodulo3.apigateway.adapter.out.security.AuthUserRepository;
import com.fabriciosanches.adamodulo3.apigateway.adapter.out.security.BCryptPasswordVerifier;
import com.fabriciosanches.adamodulo3.apigateway.adapter.out.token.JwtTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    private final AuthUserRepository userRepository;
    private final BCryptPasswordVerifier passwordVerifier;
    private final JwtTokenService tokenService;

    public LoginUseCase(AuthUserRepository userRepository, BCryptPasswordVerifier passwordVerifier, JwtTokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordVerifier = passwordVerifier;
        this.tokenService = tokenService;
    }

    public String login(String username, String password) {
        AuthUserEntity user = userRepository.findByUsernameAndEnabledTrue(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordVerifier.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return tokenService.issueToken(user.getUsername());
    }
}
