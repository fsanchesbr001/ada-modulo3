package com.fabriciosanches.adamodulo3.apigateway.adapter.out.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthUserEntity, Long> {
    Optional<AuthUserEntity> findByUsernameAndEnabledTrue(String username);
}
