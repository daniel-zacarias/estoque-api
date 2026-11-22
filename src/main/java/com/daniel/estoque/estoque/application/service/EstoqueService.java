package com.daniel.estoque.estoque.application.service;

import com.daniel.estoque.estoque.application.port.in.AjustarEstoqueUseCase;
import com.daniel.estoque.estoque.application.port.in.ConsultarMovimentacoesUseCase;
import com.daniel.estoque.estoque.application.port.in.FiltroMovimentacoes;
import com.daniel.estoque.estoque.application.port.in.MovimentacaoConsultada;
import com.daniel.estoque.estoque.application.port.in.TotaisMovimentacao;
import com.daniel.estoque.estoque.application.port.out.MovimentacaoRegistrada;
import com.daniel.estoque.estoque.application.port.out.MovimentacaoRepositoryPort;
import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import com.daniel.estoque.estoque.domain.TipoMovimento;
import com.daniel.estoque.produto.application.port.out.ProdutoRepositoryPort;
import com.daniel.estoque.produto.domain.Produto;
import com.daniel.estoque.shared.domain.RecursoNaoEncontradoException;
import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class EstoqueService implements AjustarEstoqueUseCase, ConsultarMovimentacoesUseCase {

    private final ProdutoRepositoryPort produtos;
    private final MovimentacaoRepositoryPort movimentacoes;

    EstoqueService(ProdutoRepositoryPort produtos, MovimentacaoRepositoryPort movimentacoes) {
        this.produtos = produtos;
        this.movimentacoes = movimentacoes;
    }

    /** Saldo e histórico mudam juntos, na mesma transação: o histórico sempre bate com o saldo. */
    @Override
    @Transactional
    public MovimentacaoEstoque ajustar(Comando c) {
        var produto = produtos.buscarPorId(c.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", c.produtoId()));
        int saldoAnterior = produto.qtEstoque();

        var movimentacao = MovimentacaoEstoque.ajusteManual(
                produto.idPersistido(), c.tipo(), c.motivo(), c.quantidade(), saldoAnterior, c.observacao());

        if (c.tipo() == TipoMovimento.ENTRADA) {
            produto.repor(c.quantidade());
        } else {
            produto.baixar(c.quantidade());
        }
        produtos.salvar(produto);
        movimentacoes.registrar(movimentacao);
        return movimentacao;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoConsultada> porProduto(long produtoId, Pageable paginacao) {
        return comNomes(movimentacoes.listarPorProduto(produtoId, paginacao));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoConsultada> listar(FiltroMovimentacoes filtro, Pageable paginacao) {
        return comNomes(movimentacoes.listar(filtro, paginacao));
    }

    @Override
    @Transactional(readOnly = true)
    public TotaisMovimentacao totais(Instant inicio, Instant fim) {
        if (!inicio.isBefore(fim)) {
            throw new RegraNegocioException("O início do período precisa ser antes do fim");
        }
        return movimentacoes.totais(inicio, fim);
    }

    /** Um único SELECT para os nomes da página inteira, em vez de um por movimentação. */
    private Page<MovimentacaoConsultada> comNomes(Page<MovimentacaoRegistrada> pagina) {
        var ids = pagina.getContent().stream().map(m -> m.dados().produtoId()).collect(Collectors.toSet());
        Map<Long, String> nomes = produtos.buscarPorIds(ids).stream()
                .collect(Collectors.toMap(Produto::idPersistido, Produto::nome));
        return pagina.map(m -> new MovimentacaoConsultada(m.id(), m.dados(), m.criadoEm(),
                nomes.get(m.dados().produtoId())));
    }
}
