package com.daniel.estoque.pedido.adapter.out.persistence;

import com.daniel.estoque.pedido.application.port.out.PedidoRepositoryPort;
import com.daniel.estoque.pedido.domain.Pedido;
import com.daniel.estoque.pedido.domain.StatusPedido;
import com.daniel.estoque.shared.domain.RecursoNaoEncontradoException;
import jakarta.persistence.criteria.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Component
class PedidoPersistenceAdapter implements PedidoRepositoryPort {

    private final PedidoSpringDataRepository repository;

    PedidoPersistenceAdapter(PedidoSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pedido salvar(Pedido pedido) {
        if (pedido.id() == null) {
            return repository.save(PedidoJpaEntity.novo(pedido)).paraDominio();
        }
        var entidade = repository.findComItensById(pedido.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", pedido.id()));
        entidade.atualizarStatus(pedido);
        return repository.save(entidade).paraDominio();
    }

    @Override
    public Optional<Pedido> buscarPorId(long id) {
        return repository.findComItensById(id).map(PedidoJpaEntity::paraDominio);
    }

    /**
     * Sem fetch join dos itens: com paginação, o Hibernate faria a paginação em memória. Os itens vêm
     * depois, por @BatchSize, ainda dentro da transação do caso de uso.
     */
    @Override
    public Page<Pedido> listar(@Nullable StatusPedido status, @Nullable UUID dono, Pageable paginacao) {
        Specification<PedidoJpaEntity> filtro = (root, query, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            if (status != null) {
                condicoes.add(cb.equal(root.get("status"), status));
            }
            if (dono != null) {
                condicoes.add(cb.equal(root.get("criadoPor"), dono));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return repository.findAll(filtro, paginacao).map(PedidoJpaEntity::paraDominio);
    }
}
