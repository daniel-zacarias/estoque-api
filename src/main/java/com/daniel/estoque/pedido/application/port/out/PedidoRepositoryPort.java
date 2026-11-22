package com.daniel.estoque.pedido.application.port.out;

import com.daniel.estoque.pedido.domain.Pedido;
import com.daniel.estoque.pedido.domain.StatusPedido;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PedidoRepositoryPort {

    Pedido salvar(Pedido pedido);

    Optional<Pedido> buscarPorId(long id);

    /**
     * @param status nulo = todos
     * @param dono   nulo = de qualquer usuário
     */
    Page<Pedido> listar(@Nullable StatusPedido status, @Nullable UUID dono, Pageable paginacao);
}
