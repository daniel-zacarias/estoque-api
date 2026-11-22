package com.daniel.estoque.estoque.application.port.out;

import com.daniel.estoque.estoque.application.port.in.FiltroMovimentacoes;
import com.daniel.estoque.estoque.application.port.in.TotaisMovimentacao;
import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface MovimentacaoRepositoryPort {

    void registrar(MovimentacaoEstoque movimentacao);

    Page<MovimentacaoRegistrada> listarPorProduto(long produtoId, Pageable paginacao);

    Page<MovimentacaoRegistrada> listar(FiltroMovimentacoes filtro, Pageable paginacao);

    /**
     * @param inicio inclusivo
     * @param fim    exclusivo
     */
    TotaisMovimentacao totais(Instant inicio, Instant fim);
}
