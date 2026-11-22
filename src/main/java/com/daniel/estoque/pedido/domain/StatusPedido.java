package com.daniel.estoque.pedido.domain;

public enum StatusPedido {
    /** Rascunho: o estoque ainda não foi tocado. */
    ABERTO,
    /** O estoque foi baixado. */
    CONFIRMADO,
    /** Encerrado. Se estava confirmado, o estoque foi devolvido. */
    CANCELADO
}
