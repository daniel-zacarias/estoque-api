package com.daniel.estoque.pedido.application.port.in;

import com.daniel.estoque.pedido.domain.Pedido;
import com.daniel.estoque.pedido.domain.StatusPedido;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsultarPedidoUseCase {

    /** Pedido de outro usuário é tratado como inexistente para quem não vê todos os pedidos. */
    Pedido buscar(long id, Solicitante solicitante);

    /**
     * Pedidos visíveis para o solicitante: todos para quem vê todos (ADMIN), só os próprios para os demais.
     *
     * @param status nulo = qualquer status
     */
    Page<Pedido> listar(@Nullable StatusPedido status, Solicitante solicitante, Pageable paginacao);
}
