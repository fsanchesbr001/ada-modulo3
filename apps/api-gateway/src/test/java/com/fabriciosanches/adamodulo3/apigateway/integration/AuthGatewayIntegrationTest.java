package com.fabriciosanches.adamodulo3.apigateway.integration;

import com.fabriciosanches.adamodulo3.apigateway.application.LoginUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@ActiveProfiles("test")
class AuthGatewayIntegrationTest {
    @MockBean
    private LoginUseCase loginUseCase;

    @Test
    void contextLoads() {
    }
}
