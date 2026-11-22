package com.daniel.estoque.estoque.adapter.out.persistence;

import com.daniel.estoque.estoque.application.port.out.MovimentacaoRegistrada;
import com.daniel.estoque.estoque.domain.MotivoMovimento;
import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import com.daniel.estoque.estoque.domain.TipoMovimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/** Imutável: só tem criado_por/criado_em; o Hibernate nunca gera UPDATE para ela. */
@Entity
@Immutable
@Table(name = "movimentacao_estoque")
@EntityListeners(AuditingEntityListener.class)
class MovimentacaoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimentacao")
    private Long id;

    @Column(name = "id_produto", nullable = false)
    private Long produtoId;

    @Column(name = "id_pedido")
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoMovimento motivo;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "saldo_anterior", nullable = false)
    private int saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false)
    private int saldoPosterior;

    @Column(length = 255)
    private String observacao;

    @CreatedBy
    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected MovimentacaoJpaEntity() {
    }

    MovimentacaoJpaEntity(MovimentacaoEstoque m) {
        this.produtoId = m.produtoId();
        this.pedidoId = m.pedidoId();
        this.tipo = m.tipo();
        this.motivo = m.motivo();
        this.quantidade = m.quantidade();
        this.saldoAnterior = m.saldoAnterior();
        this.saldoPosterior = m.saldoPosterior();
        this.observacao = m.observacao();
    }

    /** Só para entidades lidas do banco: id e data já existem. */
    MovimentacaoRegistrada paraRegistro() {
        return new MovimentacaoRegistrada(id, paraDominio(), criadoEm);
    }

    MovimentacaoEstoque paraDominio() {
        return new MovimentacaoEstoque(produtoId, pedidoId, tipo, motivo, quantidade, saldoAnterior, saldoPosterior, observacao);
    }
}
