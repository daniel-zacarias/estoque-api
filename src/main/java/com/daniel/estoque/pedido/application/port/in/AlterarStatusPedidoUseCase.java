package com.daniel.estoque.pedido.application.port.in;

import com.daniel.estoque.pedido.domain.Pedido;

/** Só o dono do pedido ou quem vê todos os pedidos (ADMIN) pode alterá-lo; os demais recebem "não encontrado". */
public interface AlterarStatusPedidoUseCase {

    /** Muda para CONFIRMADO e dá baixa no estoque de cada item. */
    Pedido confirmar(long pedidoId, Solicitante solicitante);

    /** Muda para CANCELADO; se estava confirmado, devolve o estoque. */
    Pedido cancelar(long pedidoId, Solicitante solicitante);
}
