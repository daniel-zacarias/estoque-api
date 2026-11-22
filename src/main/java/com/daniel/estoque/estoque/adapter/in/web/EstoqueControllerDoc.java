package com.daniel.estoque.estoque.adapter.in.web;

import com.daniel.estoque.estoque.adapter.in.web.EstoqueController.AjusteRequest;
import com.daniel.estoque.estoque.adapter.in.web.EstoqueController.MovimentacaoResponse;
import com.daniel.estoque.shared.adapter.in.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;

/** Documentação OpenAPI de {@link EstoqueController}. Mapeamento, segurança e validação ficam no controller. */
@Tag(name = "Estoque", description = "Ajustes manuais de saldo e histórico de movimentações de um produto")
interface EstoqueControllerDoc {

    @Operation(summary = "Registra um ajuste manual de estoque",
            description = "Role: **ADMIN**. Entrada ou saída fora de pedido (reposição, perda, inventário...). "
                    + "Uma saída maior que o saldo é rejeitada com 422.")
    @ApiResponse(responseCode = "201", description = "Movimentação registrada e saldo atualizado")
    MovimentacaoResponse ajustar(@Parameter(description = "Id do produto") long produtoId, AjusteRequest req);

    @Operation(summary = "Lista as movimentações de um produto",
            description = "Roles: **ADMIN**, **OPERADOR**. Ordenação permitida por `id` (padrão, desc) e `criadoEm`.")
    @ApiResponse(responseCode = "200", description = "Página de movimentações")
    PaginaResponse<MovimentacaoResponse> movimentacoes(@Parameter(description = "Id do produto") long produtoId,
                                                       @ParameterObject Pageable paginacao);
}
