package com.daniel.estoque.pedido.application.port.in;

import com.daniel.estoque.pedido.domain.Pedido;

import java.util.List;

public interface CriarPedidoUseCase {

    Pedido criar(Comando comando);

    record Comando(List<Item> itens) {
        /** Cópia imutável: quem criou o comando não consegue alterá-lo depois. */
        public Comando {
            itens = List.copyOf(itens);
        }
    }

    record Item(long produtoId, int quantidade) {
    }
}
