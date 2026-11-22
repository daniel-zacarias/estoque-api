package com.daniel.estoque.pedido.adapter.in.web;

import com.daniel.estoque.pedido.adapter.in.web.PedidoController.CriarPedidoRequest;
import com.daniel.estoque.pedido.adapter.in.web.PedidoController.PedidoResponse;
import com.daniel.estoque.pedido.adapter.in.web.PedidoController.PedidoResumoResponse;
import com.daniel.estoque.pedido.domain.StatusPedido;
import com.daniel.estoque.shared.adapter.in.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

/** Documentação OpenAPI de {@link PedidoController}. Mapeamento, segurança e validação ficam no controller. */
@Tag(name = "Pedidos", description = "Criação e ciclo de vida de pedidos (ABERTO → CONFIRMADO | CANCELADO). "
        + "Todas as operações aceitam as roles ADMIN e OPERADOR. OPERADOR só vê e altera os pedidos que criou; "
        + "pedidos de outros usuários respondem 404. ADMIN vê e altera qualquer pedido.")
interface PedidoControllerDoc {

    @Operation(summary = "Cria um pedido",
            description = "O pedido nasce ABERTO com o preço atual de cada produto. "
                    + "Cada produto pode aparecer uma única vez nos itens.")
    @ApiResponse(responseCode = "201", description = "Pedido criado. O header `Location` aponta para ele.")
    ResponseEntity<PedidoResponse> criar(CriarPedidoRequest req);

    @Operation(summary = "Lista pedidos",
            description = "OPERADOR recebe só os pedidos que criou; ADMIN recebe todos. Filtro opcional por `status`. "
                    + "Ordenação permitida por `id` (padrão, desc), `criadoEm` e `total`.")
    @ApiResponse(responseCode = "200", description = "Página de pedidos (resumo, sem os itens)")
    PaginaResponse<PedidoResumoResponse> listar(@Parameter(description = "Filtra por status") StatusPedido status,
                                                @ParameterObject Pageable paginacao,
                                                @Parameter(hidden = true) Authentication auth);

    @Operation(summary = "Busca um pedido pelo id")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "404", description = "Pedido inexistente ou de outro usuário (para OPERADOR)")
    PedidoResponse buscar(@Parameter(description = "Id do pedido") long id, @Parameter(hidden = true) Authentication auth);

    @Operation(summary = "Confirma um pedido aberto",
            description = "Baixa o estoque de cada item. Falha com 422 se o pedido não estiver ABERTO "
                    + "ou se algum produto não tiver saldo suficiente.")
    @ApiResponse(responseCode = "200", description = "Pedido confirmado")
    PedidoResponse confirmar(@Parameter(description = "Id do pedido") long id, @Parameter(hidden = true) Authentication auth);

    @Operation(summary = "Cancela um pedido",
            description = "Se o pedido já estava confirmado, o estoque dos itens é devolvido. "
                    + "Cancelar um pedido já cancelado retorna 422.")
    @ApiResponse(responseCode = "200", description = "Pedido cancelado")
    PedidoResponse cancelar(@Parameter(description = "Id do pedido") long id, @Parameter(hidden = true) Authentication auth);
}
