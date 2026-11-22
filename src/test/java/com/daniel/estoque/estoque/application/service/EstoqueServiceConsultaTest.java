package com.daniel.estoque.estoque.application.service;

import com.daniel.estoque.estoque.application.port.in.FiltroMovimentacoes;
import com.daniel.estoque.estoque.application.port.out.MovimentacaoRegistrada;
import com.daniel.estoque.estoque.application.port.out.MovimentacaoRepositoryPort;
import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import com.daniel.estoque.produto.application.port.out.ProdutoRepositoryPort;
import com.daniel.estoque.produto.domain.Produto;
import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Histórico com nome do produto e totais por período. Portas mockadas, sem Spring. */
class EstoqueServiceConsultaTest {

    private static final Instant AGORA = Instant.parse("2026-11-19T17:00:00Z");

    private final ProdutoRepositoryPort produtos = mock(ProdutoRepositoryPort.class);
    private final MovimentacaoRepositoryPort movimentacoes = mock(MovimentacaoRepositoryPort.class);
    private final EstoqueService service = new EstoqueService(produtos, movimentacoes);

    private static MovimentacaoRegistrada saida(long id, long produtoId) {
        return new MovimentacaoRegistrada(id, MovimentacaoEstoque.saidaPorPedido(produtoId, 9L, 1, 10), AGORA);
    }

    @Test
    void historicoTrazONomeDoProdutoNumaConsultaSo() {
        var paginacao = PageRequest.of(0, 20);
        given(movimentacoes.listar(any(), any())).willReturn(new PageImpl<>(
                List.of(saida(1, 10L), saida(2, 10L), saida(3, 20L)), paginacao, 3));
        given(produtos.buscarPorIds(anyCollection())).willReturn(List.of(
                Produto.reconstituir(10L, "Parafuso", null, BigDecimal.ONE, 5, null)));

        var pagina = service.listar(new FiltroMovimentacoes(null, null, null, null), paginacao);

        assertThat(pagina.getContent()).extracting(m -> m.nomeProduto()).containsExactly("Parafuso", "Parafuso", null);
        assertThat(pagina.getContent().getFirst().criadoEm()).isEqualTo(AGORA);
        verify(produtos, times(1)).buscarPorIds(anyCollection());
    }

    @Test
    void totaisExigemInicioAntesDoFim() {
        assertThatThrownBy(() -> service.totais(AGORA, AGORA)).isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(movimentacoes);
    }
}
