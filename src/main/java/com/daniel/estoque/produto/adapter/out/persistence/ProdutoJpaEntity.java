package com.daniel.estoque.produto.adapter.out.persistence;

import com.daniel.estoque.produto.domain.CodigoBarras;
import com.daniel.estoque.produto.domain.Produto;
import com.daniel.estoque.shared.adapter.out.persistence.AuditavelJpa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Modelo de persistência. Fica no adaptador; o domínio nunca vê esta classe. */
@Entity
@Table(name = "produto")
@SQLRestriction("deletado_em IS NULL")
class ProdutoJpaEntity extends AuditavelJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "codigo_barras", length = 14)
    private String codigoBarras;

    @Column(name = "qt_estoque", nullable = false)
    private int qtEstoque;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    /** Lock otimista: impede que duas baixas simultâneas sobrescrevam uma à outra. */
    @Version
    private Long versao;

    @Column(name = "deletado_em")
    private Instant deletadoEm;

    @Column(name = "deletado_por")
    private UUID deletadoPor;

    protected ProdutoJpaEntity() {
    }

    void copiarDe(Produto produto) {
        this.nome = produto.nome();
        this.descricao = produto.descricao();
        this.preco = produto.preco();
        var codigo = produto.codigoBarras();
        this.codigoBarras = codigo == null ? null : codigo.valor();
        this.qtEstoque = produto.qtEstoque();
        this.estoqueMinimo = produto.estoqueMinimo();
    }

    Produto paraDominio() {
        return Produto.reconstituir(id, nome, descricao, preco, qtEstoque, CodigoBarras.deNullable(codigoBarras),
                estoqueMinimo);
    }

    Long getId() {
        return id;
    }
}
