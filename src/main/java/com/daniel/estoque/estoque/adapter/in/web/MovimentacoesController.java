package com.daniel.estoque.estoque.adapter.in.web;

import com.daniel.estoque.estoque.adapter.in.web.EstoqueController.MovimentacaoResponse;
import com.daniel.estoque.estoque.application.port.in.ConsultarMovimentacoesUseCase;
import com.daniel.estoque.estoque.application.port.in.FiltroMovimentacoes;
import com.daniel.estoque.estoque.application.port.in.TotaisMovimentacao;
import com.daniel.estoque.estoque.domain.TipoMovimento;
import com.daniel.estoque.shared.adapter.in.web.Fuso;
import com.daniel.estoque.shared.adapter.in.web.Ordenacao;
import com.daniel.estoque.shared.adapter.in.web.PaginaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Histórico de movimentações de todos os produtos (o de um produto só fica em {@link EstoqueController}).
 * Datas chegam como dia ({@code 2026-11-19}) no fuso da operação: o "hoje" do usuário, não o do UTC.
 */
@RestController
@RequestMapping("/api/v1/movimentacoes")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
class MovimentacoesController implements MovimentacoesControllerDoc {

    private final ConsultarMovimentacoesUseCase consultar;

    MovimentacoesController(ConsultarMovimentacoesUseCase consultar) {
        this.consultar = consultar;
    }

    @Override
    @GetMapping
    public PaginaResponse<MovimentacaoResponse> listar(
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) TipoMovimento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable paginacao) {
        var filtro = new FiltroMovimentacoes(produtoId, tipo,
                de == null ? null : inicioDo(de), ate == null ? null : inicioDo(ate.plusDays(1)));
        var pagina = consultar.listar(filtro, Ordenacao.validar(paginacao, EstoqueController.ORDENACAO_PERMITIDA));
        return PaginaResponse.de(pagina, MovimentacaoResponse::deConsulta);
    }

    @Override
    @GetMapping("/totais")
    public TotaisResponse totais(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        var hoje = LocalDate.now(Fuso.OPERACAO);
        var inicio = de == null ? hoje : de;
        var fim = ate == null ? hoje : ate;
        return TotaisResponse.de(consultar.totais(inicioDo(inicio), inicioDo(fim.plusDays(1))));
    }

    private static Instant inicioDo(LocalDate dia) {
        return dia.atStartOfDay(Fuso.OPERACAO).toInstant();
    }

    @Schema(name = "TotaisMovimentacao")
    record TotaisResponse(
            @Schema(description = "Quantas movimentações de ENTRADA", example = "17") long entradas,
            @Schema(description = "Soma das quantidades de ENTRADA", example = "940") long unidadesEntrada,
            @Schema(description = "Quantas movimentações de SAIDA", example = "41") long saidas,
            @Schema(description = "Soma das quantidades de SAIDA", example = "1212") long unidadesSaida) {
        static TotaisResponse de(TotaisMovimentacao t) {
            return new TotaisResponse(t.entradas(), t.unidadesEntrada(), t.saidas(), t.unidadesSaida());
        }
    }
}
