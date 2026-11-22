package com.daniel.estoque.estoque.adapter.in.web;

import com.daniel.estoque.estoque.adapter.in.web.EstoqueController.MovimentacaoResponse;
import com.daniel.estoque.estoque.adapter.in.web.MovimentacoesController.TotaisResponse;
import com.daniel.estoque.estoque.domain.TipoMovimento;
import com.daniel.estoque.shared.adapter.in.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/** Documentação OpenAPI de {@link MovimentacoesController}. */
@Tag(name = "Estoque")
interface MovimentacoesControllerDoc {

    @Operation(summary = "Lista as movimentações de todos os produtos",
            description = "Roles: **ADMIN**, **OPERADOR**. Filtros opcionais por produto, tipo e período "
                    + "(`de` e `ate` inclusivos, em dias do fuso America/Sao_Paulo). "
                    + "Ordenação permitida por `id` (padrão, desc) e `criadoEm`.")
    @ApiResponse(responseCode = "200", description = "Página de movimentações")
    PaginaResponse<MovimentacaoResponse> listar(
            @Parameter(description = "Só deste produto") Long produtoId,
            @Parameter(description = "ENTRADA ou SAIDA") TipoMovimento tipo,
            @Parameter(description = "Primeiro dia (inclusivo), ex.: 2026-11-12") LocalDate de,
            @Parameter(description = "Último dia (inclusivo), ex.: 2026-11-19") LocalDate ate,
            @ParameterObject Pageable paginacao);

    @Operation(summary = "Soma as entradas e saídas de um período",
            description = "Roles: **ADMIN**, **OPERADOR**. Sem datas, considera o dia de hoje (America/Sao_Paulo). "
                    + "`de` depois de `ate` retorna 422.")
    @ApiResponse(responseCode = "200", description = "Totais do período")
    TotaisResponse totais(
            @Parameter(description = "Primeiro dia (inclusivo). Padrão: hoje") LocalDate de,
            @Parameter(description = "Último dia (inclusivo). Padrão: hoje") LocalDate ate);
}
