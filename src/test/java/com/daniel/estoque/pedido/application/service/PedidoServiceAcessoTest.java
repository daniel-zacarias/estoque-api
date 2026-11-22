package com.daniel.estoque.pedido.application.service;

import com.daniel.estoque.estoque.application.port.out.MovimentacaoRepositoryPort;
import com.daniel.estoque.pedido.application.port.in.Solicitante;
import com.daniel.estoque.pedido.application.port.out.PedidoRepositoryPort;
import com.daniel.estoque.pedido.domain.ItemPedido;
import com.daniel.estoque.pedido.domain.Pedido;
import com.daniel.estoque.pedido.domain.StatusPedido;
import com.daniel.estoque.produto.application.port.out.ProdutoRepositoryPort;
import com.daniel.estoque.produto.domain.Produto;
import com.daniel.estoque.shared.domain.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Quem pode ver e alterar cada pedido (OWASP API1 / BOLA). Portas mockadas, sem Spring. */
class PedidoServiceAcessoTest {

    private static final UUID DONO = UUID.randomUUID();
    private static final UUID OUTRO = UUID.randomUUID();
    private static final long ID = 7L;

    private final PedidoRepositoryPort pedidos = mock(PedidoRepositoryPort.class);
    private final ProdutoRepositoryPort produtos = mock(ProdutoRepositoryPort.class);
    private final MovimentacaoRepositoryPort movimentacoes = mock(MovimentacaoRepositoryPort.class);
    private final PedidoService service = new PedidoService(pedidos, produtos, movimentacoes);

    @BeforeEach
    void pedidoAbertoDoDono() {
        given(pedidos.buscarPorId(ID)).willReturn(Optional.of(pedidoDe(DONO)));
        given(pedidos.salvar(any())).willAnswer(inv -> inv.getArgument(0));
        given(produtos.buscarPorId(1L)).willReturn(Optional.of(
                Produto.reconstituir(1L, "Teclado", null, new BigDecimal("10.00"), 50, null)));
        given(produtos.salvar(any())).willAnswer(inv -> inv.getArgument(0));
    }

    private static Pedido pedidoDe(UUID dono) {
        return Pedido.reconstituir(ID, StatusPedido.ABERTO, List.of(new ItemPedido(1L, 2, new BigDecimal("10.00"))), dono);
    }

    private static Solicitante operador(UUID usuario) {
        return new Solicitante(usuario, false);
    }

    private static Solicitante admin() {
        return new Solicitante(UUID.randomUUID(), true);
    }

    @Test
    void donoVeConfirmaECancelaOProprioPedido() {
        assertThat(service.buscar(ID, operador(DONO)).idPersistido()).isEqualTo(ID);
        assertThat(service.confirmar(ID, operador(DONO)).status()).isEqualTo(StatusPedido.CONFIRMADO);
    }

    @Test
    void donoCancelaOProprioPedido() {
        assertThat(service.cancelar(ID, operador(DONO)).status()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void operadorNaoVePedidoDeOutroERecebeNaoEncontrado() {
        assertThatThrownBy(() -> service.buscar(ID, operador(OUTRO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void operadorNaoConfirmaPedidoDeOutroENadaMudaNoEstoque() {
        assertThatThrownBy(() -> service.confirmar(ID, operador(OUTRO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(produtos, movimentacoes);
        verify(pedidos, never()).salvar(any());
    }

    @Test
    void operadorNaoCancelaPedidoDeOutro() {
        assertThatThrownBy(() -> service.cancelar(ID, operador(OUTRO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(pedidos, never()).salvar(any());
    }

    @Test
    void adminVeEAlteraPedidoDeQualquerUm() {
        assertThat(service.buscar(ID, admin()).idPersistido()).isEqualTo(ID);
        assertThat(service.confirmar(ID, admin()).status()).isEqualTo(StatusPedido.CONFIRMADO);
    }

    @Test
    void adminCancelaPedidoDeQualquerUm() {
        assertThat(service.cancelar(ID, admin()).status()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void pedidoLegadoSemDonoSoOAdminAcessa() {
        given(pedidos.buscarPorId(ID)).willReturn(Optional.of(pedidoDe(null)));
        assertThatThrownBy(() -> service.buscar(ID, operador(DONO))).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(service.buscar(ID, admin()).idPersistido()).isEqualTo(ID);
    }

    @Test
    void operadorListaSoOsPropriosPedidos() {
        var paginacao = PageRequest.of(0, 10);
        given(pedidos.listar(any(), any(), any())).willReturn(Page.empty());
        service.listar(StatusPedido.ABERTO, operador(DONO), paginacao);
        verify(pedidos).listar(StatusPedido.ABERTO, DONO, paginacao);
    }

    @Test
    void adminListaPedidosDeTodos() {
        var paginacao = PageRequest.of(0, 10);
        given(pedidos.listar(any(), any(), any())).willReturn(Page.empty());
        service.listar(null, admin(), paginacao);
        verify(pedidos).listar(null, null, paginacao);
    }

    @Test
    void pedidoInexistenteContinuaNaoEncontradoParaTodos() {
        given(pedidos.buscarPorId(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscar(99L, admin())).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
