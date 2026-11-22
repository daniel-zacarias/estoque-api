package com.daniel.estoque.produto.adapter.out.persistence;

import com.daniel.estoque.produto.application.port.in.FiltroProdutos;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

/** Traduz {@link FiltroProdutos} para Criteria. Filtro nulo não gera condição nenhuma. */
final class ProdutoEspecificacoes {

    private ProdutoEspecificacoes() {
    }

    static Specification<ProdutoJpaEntity> de(FiltroProdutos filtro) {
        return (root, query, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            var qtEstoque = root.<Integer>get("qtEstoque");
            var estoqueMinimo = root.<Integer>get("estoqueMinimo");

            var busca = filtro.busca();
            if (busca != null) {
                // O texto do usuário vira literal: % e _ não funcionam como curinga
                var padrao = "%" + escaparLike(busca.toLowerCase(Locale.ROOT)) + "%";
                condicoes.add(cb.or(
                        cb.like(cb.lower(root.get("nome")), padrao, '\\'),
                        cb.equal(root.get("codigoBarras"), busca)));
            }

            var situacao = filtro.situacao();
            if (situacao != null) {
                condicoes.add(switch (situacao) {
                    case SEM_ESTOQUE -> cb.equal(qtEstoque, 0);
                    case SALDO_BAIXO -> cb.and(cb.greaterThan(qtEstoque, 0), cb.lessThan(qtEstoque, estoqueMinimo));
                    case NORMAL -> cb.and(cb.greaterThan(qtEstoque, 0), cb.greaterThanOrEqualTo(qtEstoque, estoqueMinimo));
                });
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }

    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
