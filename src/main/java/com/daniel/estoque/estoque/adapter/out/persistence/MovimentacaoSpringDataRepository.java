package com.daniel.estoque.estoque.adapter.out.persistence;

import com.daniel.estoque.estoque.domain.TipoMovimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface MovimentacaoSpringDataRepository extends JpaRepository<MovimentacaoJpaEntity, Long>,
        JpaSpecificationExecutor<MovimentacaoJpaEntity> {

    Page<MovimentacaoJpaEntity> findByProdutoId(Long produtoId, Pageable paginacao);

    @Query("""
            select m.tipo as tipo, count(m) as movimentacoes, coalesce(sum(m.quantidade), 0) as unidades
            from MovimentacaoJpaEntity m
            where m.criadoEm >= :inicio and m.criadoEm < :fim
            group by m.tipo""")
    List<TotalPorTipo> totaisPorTipo(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    /** Projeção do agrupamento. Number porque o tipo do count/sum depende do dialeto. */
    interface TotalPorTipo {
        TipoMovimento getTipo();

        Number getMovimentacoes();

        Number getUnidades();
    }
}
