package com.daniel.estoque.estoque.application.port.out;

import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;

import java.time.Instant;

/** Movimentação lida do banco: os dados do domínio mais o id e a data que a persistência atribuiu. */
public record MovimentacaoRegistrada(long id, MovimentacaoEstoque dados, Instant criadoEm) {
}
