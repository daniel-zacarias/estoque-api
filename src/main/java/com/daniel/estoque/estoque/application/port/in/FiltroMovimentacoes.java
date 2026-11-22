package com.daniel.estoque.estoque.application.port.in;

import com.daniel.estoque.estoque.domain.TipoMovimento;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Filtros opcionais do histórico geral de movimentações. Nulo = sem filtro.
 *
 * @param inicio inclusivo
 * @param fim    exclusivo
 */
public record FiltroMovimentacoes(@Nullable Long produtoId, @Nullable TipoMovimento tipo,
                                  @Nullable Instant inicio, @Nullable Instant fim) {
}
