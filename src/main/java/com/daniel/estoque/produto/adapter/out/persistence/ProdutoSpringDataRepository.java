package com.daniel.estoque.produto.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** As consultas derivadas respeitam o @SQLRestriction da entidade: só enxergam produtos ativos. */
interface ProdutoSpringDataRepository extends JpaRepository<ProdutoJpaEntity, Long>,
        JpaSpecificationExecutor<ProdutoJpaEntity> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);
}
