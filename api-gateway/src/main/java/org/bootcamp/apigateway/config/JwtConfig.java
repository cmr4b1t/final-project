package org.bootcamp.apigateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:abc}")
    private String secret;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {

        SecretKeySpec key =
            new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );

        return NimbusReactiveJwtDecoder
            .withSecretKey(key)
            .build();
    }
}
