package com.daniel.estoque.estoque.application.port.in;

import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Movimentação já gravada, como as telas de histórico precisam: com id, data e o nome do produto.
 *
 * @param nomeProduto nulo se o produto foi excluído depois da movimentação
 */
public record MovimentacaoConsultada(long id, MovimentacaoEstoque dados, Instant criadoEm,
                                     @Nullable String nomeProduto) {
}
