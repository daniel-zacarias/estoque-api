package com.daniel.estoque.produto.domain;

/**
 * Situação do saldo de um produto em relação ao estoque mínimo. As três faixas não se sobrepõem:
 * um produto zerado é {@link #SEM_ESTOQUE}, nunca {@link #SALDO_BAIXO}.
 */
public enum SituacaoEstoque {
    /** Saldo igual ou acima do mínimo. */
    NORMAL,
    /** Saldo maior que zero e abaixo do mínimo. */
    SALDO_BAIXO,
    /** Saldo zero. */
    SEM_ESTOQUE;

    public static SituacaoEstoque de(int qtEstoque, int estoqueMinimo) {
        if (qtEstoque == 0) {
            return SEM_ESTOQUE;
        }
        return qtEstoque < estoqueMinimo ? SALDO_BAIXO : NORMAL;
    }
}
