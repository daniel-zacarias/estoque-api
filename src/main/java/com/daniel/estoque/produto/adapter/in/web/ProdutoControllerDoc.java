package com.daniel.estoque.produto.adapter.in.web;

import com.daniel.estoque.produto.adapter.in.web.ProdutoController.AtualizarProdutoRequest;
import com.daniel.estoque.produto.adapter.in.web.ProdutoController.CadastrarProdutoRequest;
import com.daniel.estoque.produto.adapter.in.web.ProdutoController.ProdutoResponse;
import com.daniel.estoque.produto.domain.SituacaoEstoque;
import com.daniel.estoque.shared.adapter.in.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

/** Documentação OpenAPI de {@link ProdutoController}. Mapeamento, segurança e validação ficam no controller. */
@Tag(name = "Produtos", description = "Cadastro e consulta de produtos")
interface ProdutoControllerDoc {

    @Operation(summary = "Cadastra um produto",
            description = "Role: **ADMIN**. O produto nasce com estoque zero; a entrada é feita por ajuste de estoque. "
                    + "Nome e código de barras devem ser únicos entre os produtos ativos.")
    @ApiResponse(responseCode = "201", description = "Produto criado. O header `Location` aponta para ele.")
    ResponseEntity<ProdutoResponse> cadastrar(CadastrarProdutoRequest req);

    @Operation(summary = "Atualiza os dados cadastrais de um produto",
            description = "Role: **ADMIN**. Substitui nome, descrição, preço e código de barras "
                    + "(enviar `codigoBarras` nulo remove o código). O saldo (`qtEstoque`) não é alterado aqui: "
                    + "use os ajustes de estoque.")
    @ApiResponse(responseCode = "200", description = "Produto atualizado")
    ProdutoResponse atualizar(@Parameter(description = "Id do produto") long id, AtualizarProdutoRequest req);

    @Operation(summary = "Lista produtos ativos",
            description = "Roles: **ADMIN**, **OPERADOR**. Ordenação permitida por `nome` (padrão), `preco` e `qtEstoque`. "
                    + "Filtros opcionais: `busca` (parte do nome ou código de barras exato) e `situacao` "
                    + "(`SALDO_BAIXO`: acima de zero e abaixo do mínimo; `SEM_ESTOQUE`: zerado; `NORMAL`).")
    @ApiResponse(responseCode = "200", description = "Página de produtos")
    PaginaResponse<ProdutoResponse> listar(
            @Parameter(description = "Parte do nome ou código de barras exato") String busca,
            @Parameter(description = "Situação do saldo em relação ao estoque mínimo") SituacaoEstoque situacao,
            @ParameterObject Pageable paginacao);

    @Operation(summary = "Busca um produto pelo id", description = "Roles: **ADMIN**, **OPERADOR**.")
    @ApiResponse(responseCode = "200", description = "Produto encontrado")
    ProdutoResponse buscar(@Parameter(description = "Id do produto") long id);
}
