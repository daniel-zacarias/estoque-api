package com.daniel.estoque.pedido.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

interface PedidoSpringDataRepository extends JpaRepository<PedidoJpaEntity, Long>,
        JpaSpecificationExecutor<PedidoJpaEntity> {

    /** Carrega os itens junto, numa só consulta (evita N+1 e LazyInitializationException). */
    @EntityGraph(attributePaths = "itens")
    Optional<PedidoJpaEntity> findComItensById(Long id);
}
