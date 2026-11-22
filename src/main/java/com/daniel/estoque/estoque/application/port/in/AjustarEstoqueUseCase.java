package com.daniel.estoque.estoque.application.port.in;

import com.daniel.estoque.estoque.domain.MotivoMovimento;
import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import com.daniel.estoque.estoque.domain.TipoMovimento;

public interface AjustarEstoqueUseCase {

    MovimentacaoEstoque ajustar(Comando comando);

    record Comando(long produtoId, TipoMovimento tipo, MotivoMovimento motivo, int quantidade, String observacao) {
    }
}
