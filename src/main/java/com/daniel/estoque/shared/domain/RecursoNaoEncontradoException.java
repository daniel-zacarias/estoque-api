package com.daniel.estoque.shared.domain;

/** O recurso pedido não existe. O adaptador web traduz para HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " " + id + " não encontrado");
    }
}
