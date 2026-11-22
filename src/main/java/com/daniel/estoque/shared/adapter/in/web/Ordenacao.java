package com.daniel.estoque.shared.adapter.in.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/**
 * Lista branca dos campos de ordenação aceitos por endpoint. Sem ela, um campo inexistente
 * vira erro 500 e o cliente consegue ordenar por campos internos da entidade.
 */
public final class Ordenacao {

    private Ordenacao() {
    }

    public static Pageable validar(Pageable pageable, Set<String> permitidos) {
        for (Sort.Order ordem : pageable.getSort()) {
            if (!permitidos.contains(ordem.getProperty())) {
                throw new OrdenacaoInvalidaException(ordem.getProperty(), new TreeSet<>(permitidos));
            }
        }
        return pageable;
    }

    public static final class OrdenacaoInvalidaException extends RuntimeException {
        OrdenacaoInvalidaException(String campo, Set<String> permitidos) {
            super("Não é possível ordenar por '" + campo + "'. Campos permitidos: " + String.join(", ", permitidos));
        }
    }
}
