package com.daniel.estoque.estoque.adapter.out.persistence;

import com.daniel.estoque.estoque.application.port.in.FiltroMovimentacoes;
import com.daniel.estoque.estoque.application.port.in.TotaisMovimentacao;
import com.daniel.estoque.estoque.application.port.out.MovimentacaoRegistrada;
import com.daniel.estoque.estoque.application.port.out.MovimentacaoRepositoryPort;
import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import com.daniel.estoque.estoque.domain.TipoMovimento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

@Component
class MovimentacaoPersistenceAdapter implements MovimentacaoRepositoryPort {

    private final MovimentacaoSpringDataRepository repository;

    MovimentacaoPersistenceAdapter(MovimentacaoSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrar(MovimentacaoEstoque movimentacao) {
        repository.save(new MovimentacaoJpaEntity(movimentacao));
    }

    @Override
    public Page<MovimentacaoRegistrada> listarPorProduto(long produtoId, Pageable paginacao) {
        return repository.findByProdutoId(produtoId, paginacao).map(MovimentacaoJpaEntity::paraRegistro);
    }

    @Override
    public Page<MovimentacaoRegistrada> listar(FiltroMovimentacoes filtro, Pageable paginacao) {
        Specification<MovimentacaoJpaEntity> especificacao = (root, query, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            if (filtro.produtoId() != null) {
                condicoes.add(cb.equal(root.get("produtoId"), filtro.produtoId()));
            }
            if (filtro.tipo() != null) {
                condicoes.add(cb.equal(root.get("tipo"), filtro.tipo()));
            }
            if (filtro.inicio() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.<Instant>get("criadoEm"), filtro.inicio()));
            }
            if (filtro.fim() != null) {
                condicoes.add(cb.lessThan(root.<Instant>get("criadoEm"), filtro.fim()));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return repository.findAll(especificacao, paginacao).map(MovimentacaoJpaEntity::paraRegistro);
    }

    @Override
    public TotaisMovimentacao totais(Instant inicio, Instant fim) {
        long entradas = 0;
        long unidadesEntrada = 0;
        long saidas = 0;
        long unidadesSaida = 0;
        for (var total : repository.totaisPorTipo(inicio, fim)) {
            if (total.getTipo() == TipoMovimento.ENTRADA) {
                entradas = total.getMovimentacoes().longValue();
                unidadesEntrada = total.getUnidades().longValue();
            } else {
                saidas = total.getMovimentacoes().longValue();
                unidadesSaida = total.getUnidades().longValue();
            }
        }
        return new TotaisMovimentacao(entradas, unidadesEntrada, saidas, unidadesSaida);
    }
}
