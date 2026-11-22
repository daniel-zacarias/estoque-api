package com.daniel.estoque.estoque.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovimentacaoEstoqueTest {

    @Test
    void saidaPorPedidoCalculaSaldoPosterior() {
        var mov = MovimentacaoEstoque.saidaPorPedido(1, 10, 3, 8);
        assertThat(mov.saldoPosterior()).isEqualTo(5);
        assertThat(mov.tipo()).isEqualTo(TipoMovimento.SAIDA);
    }

    @Test
    void ajusteManualExigeObservacao() {
        assertThatThrownBy(() -> MovimentacaoEstoque.ajusteManual(1, TipoMovimento.ENTRADA, MotivoMovimento.REPOSICAO, 5, 0, " "))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void motivoDePedidoNaoPodeSerLancadoManualmente() {
        assertThatThrownBy(() -> MovimentacaoEstoque.ajusteManual(1, TipoMovimento.SAIDA, MotivoMovimento.PEDIDO, 1, 5, "x"))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaSaldoInconsistente() {
        assertThatThrownBy(() -> new MovimentacaoEstoque(1, null, TipoMovimento.ENTRADA, MotivoMovimento.AJUSTE, 5, 0, 4, "x"))
                .isInstanceOf(RegraNegocioException.class);
    }
}
