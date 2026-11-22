package com.daniel.estoque.produto.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;

public class EstoqueInsuficienteException extends RegraNegocioException {

    public EstoqueInsuficienteException(String produto, int disponivel, int solicitado) {
        super("Estoque insuficiente para '%s': disponível %d, solicitado %d".formatted(produto, disponivel, solicitado));
    }
}
