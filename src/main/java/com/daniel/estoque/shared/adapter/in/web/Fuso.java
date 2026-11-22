package com.daniel.estoque.shared.adapter.in.web;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Fuso da operação na borda HTTP. Internamente (domínio, banco TIMESTAMPTZ) tudo é {@link Instant} em UTC;
 * nas respostas, as datas saem com o offset de Brasília ({@code 2026-11-19T14:05:00-03:00}) para bater
 * com o que o usuário e o psql enxergam.
 */
public final class Fuso {

    /** Os usuários estão no Brasil (ADR-003). ZoneId, não offset fixo, para acompanhar eventual horário de verão. */
    public static final ZoneId OPERACAO = ZoneId.of("America/Sao_Paulo");

    private Fuso() {
    }

    public static @Nullable OffsetDateTime local(@Nullable Instant instante) {
        return instante == null ? null : instante.atZone(OPERACAO).toOffsetDateTime();
    }
}
