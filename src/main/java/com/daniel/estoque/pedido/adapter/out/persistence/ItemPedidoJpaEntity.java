package com.daniel.estoque.pedido.adapter.out.persistence;

import com.daniel.estoque.pedido.domain.ItemPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido")
class ItemPedidoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido")
    private PedidoJpaEntity pedido;

    /** Só o id: o módulo pedido não depende da entidade JPA do módulo produto. */
    @Column(name = "id_produto", nullable = false)
    private Long produtoId;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "vl_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    protected ItemPedidoJpaEntity() {
    }

    ItemPedidoJpaEntity(PedidoJpaEntity pedido, ItemPedido item) {
        this.pedido = pedido;
        this.produtoId = item.produtoId();
        this.quantidade = item.quantidade();
        this.valorUnitario = item.valorUnitario();
    }

    ItemPedido paraDominio() {
        return new ItemPedido(produtoId, quantidade, valorUnitario);
    }
}
