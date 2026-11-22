package com.daniel.estoque.produto.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Teste de domínio puro: sem Spring, sem banco, roda em milissegundos. */
class ProdutoTest {

    private Produto produtoCom(int estoque) {
        return Produto.reconstituir(1L, "Teclado", null, new BigDecimal("150.00"), estoque, null);
    }

    /** Sem a checagem, o int estoura e vira negativo (ou ArithmeticException → 500). */
    @Test
    void reporAlemDoMaximoERegraDeNegocio() {
        var produto = produtoCom(Integer.MAX_VALUE - 5);
        assertThatThrownBy(() -> produto.repor(6)).isInstanceOf(RegraNegocioException.class);
        assertThat(produto.qtEstoque()).isEqualTo(Integer.MAX_VALUE - 5);
        produto.repor(5);
        assertThat(produto.qtEstoque()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void produtoNovoComecaComEstoqueZero() {
        var produto = Produto.novo("Mouse", "Sem fio", new BigDecimal("80.00"), null);
        assertThat(produto.qtEstoque()).isZero();
        assertThat(produto.id()).isNull();
        assertThat(produto.codigoBarras()).isNull();
    }

    @Test
    void produtoNovoGuardaCodigoDeBarras() {
        var codigo = new CodigoBarras("7891000315507");
        var produto = Produto.novo("Mouse", null, new BigDecimal("80.00"), codigo);
        assertThat(produto.codigoBarras()).isEqualTo(codigo);
    }

    @Test
    void baixaReduzOSaldo() {
        var produto = produtoCom(10);
        produto.baixar(3);
        assertThat(produto.qtEstoque()).isEqualTo(7);
    }

    @Test
    void naoPermiteEstoqueNegativo() {
        var produto = produtoCom(2);
        assertThatThrownBy(() -> produto.baixar(3)).isInstanceOf(EstoqueInsuficienteException.class);
        assertThat(produto.qtEstoque()).isEqualTo(2);
    }

    @Test
    void quantidadeDeveSerPositiva() {
        var produto = produtoCom(5);
        assertThatThrownBy(() -> produto.baixar(0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> produto.repor(-1)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoPermitePrecoNegativo() {
        assertThatThrownBy(() -> Produto.novo("X", null, new BigDecimal("-1"), null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void atualizarAlteraDadosCadastraisSemMexerNoEstoque() {
        var produto = produtoCom(7);
        var codigo = new CodigoBarras("96385074");

        produto.atualizar("  Teclado mecânico ", "ABNT2", new BigDecimal("299.90"), codigo);

        assertThat(produto.nome()).isEqualTo("Teclado mecânico");
        assertThat(produto.descricao()).isEqualTo("ABNT2");
        assertThat(produto.preco()).isEqualByComparingTo("299.90");
        assertThat(produto.codigoBarras()).isEqualTo(codigo);
        assertThat(produto.qtEstoque()).isEqualTo(7);
    }

    @Test
    void atualizarPodeRemoverCodigoDeBarras() {
        var produto = Produto.reconstituir(1L, "Teclado", null, BigDecimal.TEN, 0, new CodigoBarras("96385074"));
        produto.atualizar("Teclado", null, BigDecimal.TEN, null);
        assertThat(produto.codigoBarras()).isNull();
    }

    @Test
    void situacaoComparaOSaldoComOMinimo() {
        assertThat(Produto.reconstituir(1L, "A", null, BigDecimal.ONE, 0, null, 5).situacao())
                .isEqualTo(SituacaoEstoque.SEM_ESTOQUE);
        assertThat(Produto.reconstituir(1L, "A", null, BigDecimal.ONE, 4, null, 5).situacao())
                .isEqualTo(SituacaoEstoque.SALDO_BAIXO);
        assertThat(Produto.reconstituir(1L, "A", null, BigDecimal.ONE, 5, null, 5).situacao())
                .isEqualTo(SituacaoEstoque.NORMAL);
    }

    @Test
    void semMinimoNuncaFicaComSaldoBaixo() {
        assertThat(produtoCom(1).situacao()).isEqualTo(SituacaoEstoque.NORMAL);
        assertThat(produtoCom(0).situacao()).isEqualTo(SituacaoEstoque.SEM_ESTOQUE);
    }

    @Test
    void estoqueMinimoNaoPodeSerNegativo() {
        assertThatThrownBy(() -> Produto.novo("X", null, BigDecimal.ONE, null, -1))
                .isInstanceOf(RegraNegocioException.class);
        var produto = produtoCom(3);
        assertThatThrownBy(() -> produto.atualizar("Outro", null, BigDecimal.TEN, null, -1))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(produto.nome()).isEqualTo("Teclado");
        assertThat(produto.estoqueMinimo()).isZero();
    }

    @Test
    void atualizarTrocaOEstoqueMinimo() {
        var produto = produtoCom(3);
        produto.atualizar("Teclado", null, BigDecimal.TEN, null, 10);
        assertThat(produto.estoqueMinimo()).isEqualTo(10);
        assertThat(produto.situacao()).isEqualTo(SituacaoEstoque.SALDO_BAIXO);
    }

    @Test
    void atualizarSemMinimoMantemOAtual() {
        var produto = Produto.reconstituir(1L, "Teclado", null, BigDecimal.TEN, 3, null, 8);
        produto.atualizar("Teclado", null, BigDecimal.TEN, null);
        assertThat(produto.estoqueMinimo()).isEqualTo(8);
    }

    @Test
    void atualizarComDadosInvalidosNaoAlteraNada() {
        var produto = produtoCom(1);
        assertThatThrownBy(() -> produto.atualizar(" ", null, BigDecimal.ONE, null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> produto.atualizar("Outro", null, new BigDecimal("-1"), null))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(produto.nome()).isEqualTo("Teclado");
        assertThat(produto.preco()).isEqualByComparingTo("150.00");
    }
}
