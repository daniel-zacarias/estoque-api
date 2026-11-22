package com.daniel.estoque.pedido.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    private static ItemPedido item(long produtoId, int qtd, String valor) {
        return new ItemPedido(produtoId, qtd, new BigDecimal(valor));
    }

    @Test
    void pertenceSoAoDono() {
        var dono = java.util.UUID.randomUUID();
        var pedido = Pedido.reconstituir(1L, StatusPedido.ABERTO, List.of(item(1, 1, "1")), dono);
        assertThat(pedido.pertenceA(dono)).isTrue();
        assertThat(pedido.pertenceA(java.util.UUID.randomUUID())).isFalse();
    }

    @Test
    void pedidoSemDonoNaoPertenceANinguem() {
        var pedido = Pedido.reconstituir(1L, StatusPedido.ABERTO, List.of(item(1, 1, "1")), null);
        assertThat(pedido.pertenceA(java.util.UUID.randomUUID())).isFalse();
    }

    @Test
    void totalEhASomaDosSubtotais() {
        var pedido = Pedido.criar(List.of(item(1, 2, "10.00"), item(2, 1, "5.50")));
        assertThat(pedido.total()).isEqualByComparingTo("25.50");
        assertThat(pedido.status()).isEqualTo(StatusPedido.ABERTO);
    }

    @Test
    void naoAceitaPedidoVazioNemProdutoRepetido() {
        assertThatThrownBy(() -> Pedido.criar(List.of())).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> Pedido.criar(List.of(item(1, 1, "1"), item(1, 2, "1"))))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void soConfirmaPedidoAberto() {
        var pedido = Pedido.criar(List.of(item(1, 1, "1")));
        pedido.confirmar();
        assertThat(pedido.status()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThatThrownBy(pedido::confirmar).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cancelarPedidoConfirmadoExigeEstorno() {
        var pedido = Pedido.criar(List.of(item(1, 1, "1")));
        pedido.confirmar();
        assertThat(pedido.cancelar()).isTrue();
        assertThat(pedido.status()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void cancelarPedidoAbertoNaoExigeEstorno() {
        var pedido = Pedido.criar(List.of(item(1, 1, "1")));
        assertThat(pedido.cancelar()).isFalse();
        assertThatThrownBy(pedido::cancelar).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void itensSaoProcessadosEmOrdemDeProduto() {
        var pedido = Pedido.criar(List.of(item(9, 1, "1"), item(3, 1, "1"), item(5, 1, "1")));
        assertThat(pedido.itensEmOrdemDeProduto()).extracting(ItemPedido::produtoId).containsExactly(3L, 5L, 9L);
    }
}
