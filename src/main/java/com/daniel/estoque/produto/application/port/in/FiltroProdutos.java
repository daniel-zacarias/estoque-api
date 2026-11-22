package com.daniel.estoque.produto.application.port.in;

import com.daniel.estoque.produto.domain.SituacaoEstoque;
import org.jspecify.annotations.Nullable;

/**
 * Filtros opcionais da listagem de produtos. Nulo = sem filtro.
 *
 * @param busca     parte do nome (sem diferenciar maiúsculas) ou o código de barras exato
 * @param situacao  faixa do saldo em relação ao estoque mínimo
 */
public record FiltroProdutos(@Nullable String busca, @Nullable SituacaoEstoque situacao) {

    public static final FiltroProdutos NENHUM = new FiltroProdutos(null, null);

    public FiltroProdutos {
        busca = busca == null || busca.isBlank() ? null : busca.strip();
    }
}
