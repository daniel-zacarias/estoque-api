package com.daniel.estoque.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteRequisicoesFilterTest {

    /** Relógio controlado pelo teste; começa no início de um minuto. */
    private final AtomicLong agora = new AtomicLong(1_800_000_000_000L - 1_800_000_000_000L % 60_000);
    private final Clock relogio = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(agora.get()); }
    };
    private final LimiteRequisicoesFilter filtro = new LimiteRequisicoesFilter(3, relogio);

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private int chamarComo(String sub) throws Exception {
        var jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject(sub).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        var resposta = new MockHttpServletResponse();
        filtro.doFilter(new MockHttpServletRequest("GET", "/api/v1/produtos"), resposta, new MockFilterChain());
        return resposta.getStatus();
    }

    @Test
    void bloqueiaAcimaDoLimiteNaMesmaJanela() throws Exception {
        assertThat(chamarComo("a")).isEqualTo(200);
        assertThat(chamarComo("a")).isEqualTo(200);
        assertThat(chamarComo("a")).isEqualTo(200);
        assertThat(chamarComo("a")).isEqualTo(429);
    }

    @Test
    void respostaDe429TrazRetryAfterEProblemDetail() throws Exception {
        for (int i = 0; i < 3; i++) chamarComo("a");
        agora.addAndGet(45_000);
        var jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("a").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        var resposta = new MockHttpServletResponse();
        filtro.doFilter(new MockHttpServletRequest(), resposta, new MockFilterChain());

        assertThat(resposta.getStatus()).isEqualTo(429);
        assertThat(resposta.getHeader("Retry-After")).isEqualTo("15");
        assertThat(resposta.getContentType()).startsWith("application/problem+json");
        assertThat(resposta.getContentAsString()).contains("\"status\":429");
    }

    @Test
    void contaCadaUsuarioSeparadamente() throws Exception {
        for (int i = 0; i < 3; i++) chamarComo("a");
        assertThat(chamarComo("b")).isEqualTo(200);
    }

    @Test
    void liberaNaJanelaSeguinte() throws Exception {
        for (int i = 0; i < 4; i++) chamarComo("a");
        agora.addAndGet(60_000);
        assertThat(chamarComo("a")).isEqualTo(200);
    }

    @Test
    void naoContaRequisicoesSemToken() throws Exception {
        for (int i = 0; i < 10; i++) {
            var resposta = new MockHttpServletResponse();
            filtro.doFilter(new MockHttpServletRequest(), resposta, new MockFilterChain());
            assertThat(resposta.getStatus()).isEqualTo(200);
        }
    }
}
