package com.daniel.estoque.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Preenche criado_por / atualizado_por com o "sub" do token (UUID do usuário no Keycloak).
 * O domínio não sabe nada disso: a auditoria é responsabilidade da persistência.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAtual")
class AuditoriaConfig {

    @Bean
    AuditorAware<UUID> auditorAtual() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getPrincipal() instanceof Jwt jwt ? jwt.getSubject() : null)
                .map(UUID::fromString);
    }
}
