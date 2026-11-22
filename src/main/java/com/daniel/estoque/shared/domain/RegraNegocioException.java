package com.daniel.estoque.shared.domain;

/** Violação de uma regra de negócio. O adaptador web traduz para HTTP 422. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
