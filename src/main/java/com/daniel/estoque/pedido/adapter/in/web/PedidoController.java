package com.daniel.estoque.pedido.adapter.in.web;

import com.daniel.estoque.pedido.application.port.in.AlterarStatusPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.ConsultarPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.CriarPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.Solicitante;
import com.daniel.estoque.pedido.domain.ItemPedido;
import com.daniel.estoque.pedido.domain.Pedido;
import com.daniel.estoque.pedido.domain.StatusPedido;
import com.daniel.estoque.shared.adapter.in.web.Fuso;
import com.daniel.estoque.shared.adapter.in.web.Ordenacao;
import com.daniel.estoque.shared.adapter.in.web.PaginaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pedidos")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
class PedidoController implements PedidoControllerDoc {

    private static final Set<String> ORDENACAO_PERMITIDA = Set.of("id", "criadoEm", "total");

    private final CriarPedidoUseCase criar;
    private final AlterarStatusPedidoUseCase alterarStatus;
    private final ConsultarPedidoUseCase consultar;

    PedidoController(CriarPedidoUseCase criar, AlterarStatusPedidoUseCase alterarStatus, ConsultarPedidoUseCase consultar) {
        this.criar = criar;
        this.alterarStatus = alterarStatus;
        this.consultar = consultar;
    }

    @Override
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody CriarPedidoRequest req) {
        var comando = new CriarPedidoUseCase.Comando(req.itens().stream()
                .map(i -> new CriarPedidoUseCase.Item(i.produtoId(), i.quantidade()))
                .toList());
        var pedido = criar.criar(comando);
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + pedido.idPersistido()))
                .body(PedidoResponse.de(pedido));
    }

    /** OPERADOR lista só os pedidos que criou; ADMIN, todos. Mais novos primeiro. */
    @Override
    @GetMapping
    public PaginaResponse<PedidoResumoResponse> listar(@RequestParam(required = false) StatusPedido status,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable paginacao, Authentication auth) {
        var pagina = consultar.listar(status, solicitante(auth), Ordenacao.validar(paginacao, ORDENACAO_PERMITIDA));
        return PaginaResponse.de(pagina, PedidoResumoResponse::de);
    }

    @Override
    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable long id, Authentication auth) {
        return PedidoResponse.de(consultar.buscar(id, solicitante(auth)));
    }

    @Override
    @PostMapping("/{id}/confirmar")
    public PedidoResponse confirmar(@PathVariable long id, Authentication auth) {
        return PedidoResponse.de(alterarStatus.confirmar(id, solicitante(auth)));
    }

    @Override
    @PostMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable long id, Authentication auth) {
        return PedidoResponse.de(alterarStatus.cancelar(id, solicitante(auth)));
    }

    /** Traduz o token para a linguagem da aplicação: quem é ("sub") e se vê todos os pedidos (ADMIN). */
    private static Solicitante solicitante(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new Solicitante(UUID.fromString(auth.getName()), admin);
    }

    @Schema(name = "CriarPedidoRequest")
    record CriarPedidoRequest(@NotEmpty @Size(max = MAX_ITENS) List<@Valid ItemRequest> itens) {
        /** Cada item gera consultas e locks no banco: um teto evita pedidos gigantes (OWASP API4). */
        static final int MAX_ITENS = 100;
    }

    @Schema(name = "ItemPedidoRequest")
    record ItemRequest(
            @Schema(example = "1") @NotNull Long produtoId,
            @Schema(example = "2") @Positive int quantidade) {
    }

    @Schema(name = "Pedido")
    record PedidoResponse(
            @Schema(example = "1") long id,
            StatusPedido status,
            @Schema(example = "599.80") BigDecimal total,
            List<ItemResponse> itens,
            @Schema(example = "2026-11-19T14:05:00-03:00", nullable = true) OffsetDateTime criadoEm) {
        static PedidoResponse de(Pedido p) {
            return new PedidoResponse(p.idPersistido(), p.status(), p.total(),
                    p.itens().stream().map(ItemResponse::de).toList(), Fuso.local(p.criadoEm()));
        }
    }

    @Schema(name = "PedidoResumo")
    record PedidoResumoResponse(
            @Schema(example = "1") long id,
            StatusPedido status,
            @Schema(example = "599.80") BigDecimal total,
            @Schema(description = "Quantos produtos diferentes o pedido tem", example = "3") int quantidadeItens,
            @Schema(example = "2026-11-19T14:05:00-03:00", nullable = true) OffsetDateTime criadoEm) {
        static PedidoResumoResponse de(Pedido p) {
            return new PedidoResumoResponse(p.idPersistido(), p.status(), p.total(), p.itens().size(), Fuso.local(p.criadoEm()));
        }
    }

    @Schema(name = "ItemPedido")
    record ItemResponse(
            @Schema(example = "1") long produtoId,
            @Schema(example = "2") int quantidade,
            @Schema(description = "Preço do produto no momento do pedido", example = "299.90") BigDecimal valorUnitario,
            @Schema(example = "599.80") BigDecimal subtotal) {
        static ItemResponse de(ItemPedido i) {
            return new ItemResponse(i.produtoId(), i.quantidade(), i.valorUnitario(), i.subtotal());
        }
    }
}
