package com.daniel.estoque.estoque.adapter.in.web;

import com.daniel.estoque.estoque.application.port.in.AjustarEstoqueUseCase;
import com.daniel.estoque.estoque.application.port.in.ConsultarMovimentacoesUseCase;
import com.daniel.estoque.estoque.application.port.in.MovimentacaoConsultada;
import com.daniel.estoque.estoque.domain.MotivoMovimento;
import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import com.daniel.estoque.estoque.domain.TipoMovimento;
import com.daniel.estoque.shared.adapter.in.web.Fuso;
import com.daniel.estoque.shared.adapter.in.web.Ordenacao;
import com.daniel.estoque.shared.adapter.in.web.PaginaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/produtos/{produtoId}")
class EstoqueController implements EstoqueControllerDoc {

    static final Set<String> ORDENACAO_PERMITIDA = Set.of("id", "criadoEm");

    private final AjustarEstoqueUseCase ajustar;
    private final ConsultarMovimentacoesUseCase consultar;

    EstoqueController(AjustarEstoqueUseCase ajustar, ConsultarMovimentacoesUseCase consultar) {
        this.ajustar = ajustar;
        this.consultar = consultar;
    }

    /** Entrada ou saída manual (reposição, perda, inventário...). Só ADMIN. */
    @Override
    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MovimentacaoResponse ajustar(@PathVariable long produtoId, @Valid @RequestBody AjusteRequest req) {
        var mov = ajustar.ajustar(new AjustarEstoqueUseCase.Comando(
                produtoId, req.tipo(), req.motivo(), req.quantidade(), req.observacao()));
        return MovimentacaoResponse.de(mov);
    }

    @Override
    @GetMapping("/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public PaginaResponse<MovimentacaoResponse> movimentacoes(@PathVariable long produtoId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable paginacao) {
        var pagina = consultar.porProduto(produtoId, Ordenacao.validar(paginacao, ORDENACAO_PERMITIDA));
        return PaginaResponse.de(pagina, MovimentacaoResponse::deConsulta);
    }

    @Schema(name = "AjusteEstoqueRequest")
    record AjusteRequest(
            @Schema(example = "ENTRADA") @NotNull TipoMovimento tipo,
            @Schema(example = "REPOSICAO") @NotNull MotivoMovimento motivo,
            @Schema(example = "10") @Positive int quantidade,
            @Schema(example = "Compra NF 1234") @NotBlank @Size(max = 255) String observacao) {
    }

    @Schema(name = "Movimentacao")
    record MovimentacaoResponse(
            @Schema(description = "Nulo na resposta do ajuste", example = "42", nullable = true) Long id,
            @Schema(example = "1") long produtoId,
            @Schema(description = "Nulo se o produto foi excluído (e na resposta do ajuste)",
                    example = "Teclado mecânico", nullable = true) String nomeProduto,
            @Schema(description = "Pedido que originou a movimentação, se houver", nullable = true) Long pedidoId,
            TipoMovimento tipo,
            MotivoMovimento motivo,
            @Schema(example = "10") int quantidade,
            @Schema(example = "2") int saldoAnterior,
            @Schema(example = "12") int saldoPosterior,
            @Schema(example = "Compra NF 1234", nullable = true) String observacao,
            @Schema(description = "Nulo na resposta do ajuste", example = "2026-11-19T14:05:00-03:00", nullable = true)
            OffsetDateTime criadoEm) {

        /** Resposta do ajuste: a movimentação acabou de ser criada, sem id e data lidos do banco. */
        static MovimentacaoResponse de(MovimentacaoEstoque m) {
            return new MovimentacaoResponse(null, m.produtoId(), null, m.pedidoId(), m.tipo(), m.motivo(),
                    m.quantidade(), m.saldoAnterior(), m.saldoPosterior(), m.observacao(), null);
        }

        static MovimentacaoResponse deConsulta(MovimentacaoConsultada c) {
            var m = c.dados();
            return new MovimentacaoResponse(c.id(), m.produtoId(), c.nomeProduto(), m.pedidoId(), m.tipo(), m.motivo(),
                    m.quantidade(), m.saldoAnterior(), m.saldoPosterior(), m.observacao(), Fuso.local(c.criadoEm()));
        }
    }
}
