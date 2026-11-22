package com.daniel.estoque.estoque.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface ConsultarMovimentacoesUseCase {

    Page<MovimentacaoConsultada> porProduto(long produtoId, Pageable paginacao);

    /** Histórico de todos os produtos, com filtros opcionais. */
    Page<MovimentacaoConsultada> listar(FiltroMovimentacoes filtro, Pageable paginacao);

    /**
     * @param inicio inclusivo
     * @param fim    exclusivo
     */
    TotaisMovimentacao totais(Instant inicio, Instant fim);
}
