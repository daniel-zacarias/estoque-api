package com.daniel.estoque.produto.adapter.in.web;

import com.daniel.estoque.produto.application.port.in.AtualizarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.CadastrarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.ConsultarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.FiltroProdutos;
import com.daniel.estoque.produto.domain.Produto;
import com.daniel.estoque.produto.domain.SituacaoEstoque;
import com.daniel.estoque.shared.adapter.in.web.Ordenacao;
import com.daniel.estoque.shared.adapter.in.web.PaginaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/produtos")
class ProdutoController implements ProdutoControllerDoc {

    private static final Set<String> ORDENACAO_PERMITIDA = Set.of("nome", "preco", "qtEstoque");
    /** Texto de busca maior que o nome mais longo possível não encontra nada: corta antes de ir ao banco. */
    private static final int BUSCA_MAX = 120;

    private final CadastrarProdutoUseCase cadastrar;
    private final ConsultarProdutoUseCase consultar;
    private final AtualizarProdutoUseCase atualizar;

    ProdutoController(CadastrarProdutoUseCase cadastrar, ConsultarProdutoUseCase consultar,
                      AtualizarProdutoUseCase atualizar) {
        this.cadastrar = cadastrar;
        this.consultar = consultar;
        this.atualizar = atualizar;
    }

    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProdutoResponse> cadastrar(@Valid @RequestBody CadastrarProdutoRequest req) {
        var produto = cadastrar.cadastrar(new CadastrarProdutoUseCase.Comando(
                req.nome(), req.descricao(), req.preco(), req.codigoBarras(), req.estoqueMinimo()));
        return ResponseEntity.created(URI.create("/api/v1/produtos/" + produto.idPersistido()))
                .body(ProdutoResponse.de(produto));
    }

    /** Substitui os dados cadastrais. qtEstoque não é editável aqui: use as movimentações de estoque. */
    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProdutoResponse atualizar(@PathVariable long id, @Valid @RequestBody AtualizarProdutoRequest req) {
        var produto = atualizar.atualizar(id, new AtualizarProdutoUseCase.Comando(
                req.nome(), req.descricao(), req.preco(), req.codigoBarras(), req.estoqueMinimo()));
        return ProdutoResponse.de(produto);
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public PaginaResponse<ProdutoResponse> listar(@RequestParam(required = false) String busca,
                                                  @RequestParam(required = false) SituacaoEstoque situacao,
                                                  @PageableDefault(sort = "nome") Pageable paginacao) {
        var texto = busca != null && busca.length() > BUSCA_MAX ? busca.substring(0, BUSCA_MAX) : busca;
        var pagina = consultar.listar(new FiltroProdutos(texto, situacao),
                Ordenacao.validar(paginacao, ORDENACAO_PERMITIDA));
        return PaginaResponse.de(pagina, ProdutoResponse::de);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ProdutoResponse buscar(@PathVariable long id) {
        return ProdutoResponse.de(consultar.buscar(id));
    }

    private static final String DESCRICAO_CODIGO_BARRAS =
            "GTIN opcional (8, 12, 13 ou 14 dígitos) com dígito verificador válido. Único entre produtos ativos.";

    @Schema(name = "CadastrarProdutoRequest")
    record CadastrarProdutoRequest(
            @Schema(example = "Teclado mecânico") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "ABNT2, switch marrom") @Size(max = 500) String descricao,
            @Schema(example = "299.90") @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal preco,
            @Schema(description = DESCRICAO_CODIGO_BARRAS, example = "7891000315507", nullable = true)
            @Size(max = 14) String codigoBarras,
            @Schema(description = "Abaixo dele o produto aparece como saldo baixo. Nulo ou 0 desliga o alerta.",
                    example = "5", nullable = true)
            @PositiveOrZero Integer estoqueMinimo) {
    }

    @Schema(name = "AtualizarProdutoRequest")
    record AtualizarProdutoRequest(
            @Schema(example = "Teclado mecânico") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "ABNT2, switch marrom") @Size(max = 500) String descricao,
            @Schema(example = "279.90") @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal preco,
            @Schema(description = DESCRICAO_CODIGO_BARRAS + " Nulo remove o código.", example = "7891000315507",
                    nullable = true)
            @Size(max = 14) String codigoBarras,
            @Schema(description = "Abaixo dele o produto aparece como saldo baixo. 0 desliga o alerta; nulo mantém o atual.",
                    example = "5", nullable = true)
            @PositiveOrZero Integer estoqueMinimo) {
    }

    @Schema(name = "Produto")
    record ProdutoResponse(
            @Schema(example = "1") long id,
            @Schema(example = "Teclado mecânico") String nome,
            @Schema(example = "ABNT2, switch marrom", nullable = true) String descricao,
            @Schema(example = "299.90") BigDecimal preco,
            @Schema(example = "7891000315507", nullable = true) String codigoBarras,
            @Schema(description = "Saldo atual em estoque", example = "12") int qtEstoque,
            @Schema(description = "Estoque mínimo (0 = sem alerta)", example = "5") int estoqueMinimo,
            @Schema(description = "Saldo em relação ao mínimo") SituacaoEstoque situacao) {
        static ProdutoResponse de(Produto p) {
            var codigo = p.codigoBarras();
            var codigoBarras = codigo == null ? null : codigo.valor();
            return new ProdutoResponse(p.idPersistido(), p.nome(), p.descricao(), p.preco(), codigoBarras,
                    p.qtEstoque(), p.estoqueMinimo(), p.situacao());
        }
    }
}
