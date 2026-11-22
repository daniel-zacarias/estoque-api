package com.daniel.estoque.estoque.domain;

public enum MotivoMovimento {
    PEDIDO(false),
    CANCELAMENTO_PEDIDO(false),
    AJUSTE(true),
    PERDA(true),
    INVENTARIO(true),
    REPOSICAO(true);

    private final boolean manual;

    MotivoMovimento(boolean manual) {
        this.manual = manual;
    }

    /** Motivos que um usuário pode lançar à mão. PEDIDO e CANCELAMENTO_PEDIDO só o sistema gera. */
    public boolean permitidoEmAjusteManual() {
        return manual;
    }
}
