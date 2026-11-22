package com.daniel.estoque.pedido.adapter.out.persistence;

import com.daniel.estoque.pedido.domain.Pedido;
import com.daniel.estoque.pedido.domain.StatusPedido;
import com.daniel.estoque.shared.adapter.out.persistence.AuditavelJpa;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
class PedidoJpaEntity extends AuditavelJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status;

    /** Snapshot do total, calculado pelo domínio a partir dos itens. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Version
    private Long versao;

    /** Na listagem, os itens de uma página inteira vêm numa consulta só (em vez de uma por pedido). */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<ItemPedidoJpaEntity> itens = new ArrayList<>();

    protected PedidoJpaEntity() {
    }

    /** Novo pedido: grava cabeçalho e itens. */
    static PedidoJpaEntity novo(Pedido pedido) {
        var entidade = new PedidoJpaEntity();
        entidade.status = pedido.status();
        entidade.total = pedido.total();
        pedido.itens().forEach(item -> entidade.itens.add(new ItemPedidoJpaEntity(entidade, item)));
        return entidade;
    }

    /** Pedido existente: os itens são imutáveis depois de criados, só o status muda. */
    void atualizarStatus(Pedido pedido) {
        this.status = pedido.status();
    }

    Pedido paraDominio() {
        return Pedido.reconstituir(id, status, itens.stream().map(ItemPedidoJpaEntity::paraDominio).toList(),
                getCriadoPor(), getCriadoEm());
    }
}
