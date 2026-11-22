package com.daniel.estoque.pedido.application.service;

import com.daniel.estoque.estoque.application.port.out.MovimentacaoRepositoryPort;
import com.daniel.estoque.estoque.domain.MovimentacaoEstoque;
import com.daniel.estoque.pedido.application.port.in.AlterarStatusPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.ConsultarPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.CriarPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.Solicitante;
import com.daniel.estoque.pedido.application.port.out.PedidoRepositoryPort;
import com.daniel.estoque.pedido.domain.ItemPedido;
import com.daniel.estoque.pedido.domain.Pedido;
import com.daniel.estoque.pedido.domain.StatusPedido;
import com.daniel.estoque.produto.application.port.out.ProdutoRepositoryPort;
import com.daniel.estoque.produto.domain.Produto;
import com.daniel.estoque.shared.domain.RecursoNaoEncontradoException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PedidoService implements CriarPedidoUseCase, AlterarStatusPedidoUseCase, ConsultarPedidoUseCase {

    private final PedidoRepositoryPort pedidos;
    private final ProdutoRepositoryPort produtos;
    private final MovimentacaoRepositoryPort movimentacoes;

    PedidoService(PedidoRepositoryPort pedidos, ProdutoRepositoryPort produtos, MovimentacaoRepositoryPort movimentacoes) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.movimentacoes = movimentacoes;
    }

    @Override
    @Transactional
    public Pedido criar(Comando comando) {
        var itens = comando.itens().stream()
                .map(i -> new ItemPedido(i.produtoId(), i.quantidade(), produto(i.produtoId()).preco()))
                .toList();
        return pedidos.salvar(Pedido.criar(itens));
    }

    /**
     * Tudo numa transação: baixa de cada produto, uma movimentação de SAIDA por item e a mudança de status.
     * Se qualquer item não tiver estoque, nada é gravado.
     */
    @Override
    @Transactional
    public Pedido confirmar(long pedidoId, Solicitante solicitante) {
        var pedido = pedidoAcessivel(pedidoId, solicitante);
        pedido.confirmar();
        for (var item : pedido.itensEmOrdemDeProduto()) {
            var produto = produto(item.produtoId());
            int saldoAnterior = produto.qtEstoque();
            produto.baixar(item.quantidade());
            produtos.salvar(produto);
            movimentacoes.registrar(MovimentacaoEstoque.saidaPorPedido(
                    produto.idPersistido(), pedido.idPersistido(), item.quantidade(), saldoAnterior));
        }
        return pedidos.salvar(pedido);
    }

    @Override
    @Transactional
    public Pedido cancelar(long pedidoId, Solicitante solicitante) {
        var pedido = pedidoAcessivel(pedidoId, solicitante);
        if (pedido.cancelar()) {
            for (var item : pedido.itensEmOrdemDeProduto()) {
                var produto = produto(item.produtoId());
                int saldoAnterior = produto.qtEstoque();
                produto.repor(item.quantidade());
                produtos.salvar(produto);
                movimentacoes.registrar(MovimentacaoEstoque.estornoDePedido(
                        produto.idPersistido(), pedido.idPersistido(), item.quantidade(), saldoAnterior));
            }
        }
        return pedidos.salvar(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido buscar(long id, Solicitante solicitante) {
        return pedidoAcessivel(id, solicitante);
    }

    /**
     * Pedido de outro usuário responde igual a um inexistente (404, não 403): assim não dá para
     * descobrir quais ids existem testando um por um (OWASP API1 / BOLA).
     */
    /** Mesma regra do {@link #buscar}: quem não vê todos só lista os próprios (OWASP API1 / BOLA). */
    @Override
    @Transactional(readOnly = true)
    public Page<Pedido> listar(@Nullable StatusPedido status, Solicitante solicitante, Pageable paginacao) {
        var dono = solicitante.veTodosOsPedidos() ? null : solicitante.usuario();
        return pedidos.listar(status, dono, paginacao);
    }

    private Pedido pedidoAcessivel(long id, Solicitante solicitante) {
        return pedidos.buscarPorId(id)
                .filter(p -> solicitante.veTodosOsPedidos() || p.pertenceA(solicitante.usuario()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }

    private Produto produto(long id) {
        return produtos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }
}
