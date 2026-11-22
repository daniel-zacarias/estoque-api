package com.daniel.estoque.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de requisições por usuário autenticado, em janela fixa de 1 minuto (OWASP API4).
 *
 * <p>Conta pelo "sub" do token, não por IP: atrás do Caddy todos chegam do mesmo IP, e assim o mapa só
 * cresce com usuários reais (requisições sem token válido já param no 401 antes daqui).
 * O estado é em memória: vale para uma instância só da API, que é o cenário da VPS (ADR-003).
 *
 * <p>Não é um bean de propósito: se fosse, o Spring Boot também o registraria fora da cadeia de segurança.
 */
final class LimiteRequisicoesFilter extends OncePerRequestFilter {

    private static final long MINUTO_MS = 60_000;

    private final int limitePorMinuto;
    private final Clock relogio;
    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();

    LimiteRequisicoesFilter(int limitePorMinuto, Clock relogio) {
        if (limitePorMinuto <= 0) {
            throw new IllegalArgumentException("Limite por minuto deve ser positivo");
        }
        this.limitePorMinuto = limitePorMinuto;
        this.relogio = relogio;
    }

    private record Janela(long minuto, int contagem) {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
            chain.doFilter(request, response);
            return;
        }
        long agora = relogio.millis();
        long minutoAtual = agora / MINUTO_MS;
        var janela = janelas.compute(token.getName(), (usuario, anterior) ->
                anterior == null || anterior.minuto() != minutoAtual
                        ? new Janela(minutoAtual, 1)
                        : new Janela(minutoAtual, anterior.contagem() + 1));

        if (janela.contagem() > limitePorMinuto) {
            long segundosAteLiberar = Math.max(1, ((minutoAtual + 1) * MINUTO_MS - agora + 999) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(segundosAteLiberar));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Too Many Requests","status":429,\
                    "detail":"Limite de %d requisições por minuto excedido. Tente novamente em %d s."}"""
                    .formatted(limitePorMinuto, segundosAteLiberar));
            return;
        }
        chain.doFilter(request, response);
    }
}
