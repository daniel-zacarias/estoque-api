package com.daniel.estoque.produto.application.service;

import com.daniel.estoque.produto.application.port.in.AtualizarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.CadastrarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.ConsultarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.FiltroProdutos;
import com.daniel.estoque.produto.application.port.out.ProdutoRepositoryPort;
import com.daniel.estoque.produto.domain.CodigoBarras;
import com.daniel.estoque.produto.domain.Produto;
import com.daniel.estoque.shared.domain.RecursoNaoEncontradoException;
import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD simples: hexagonal "pragmática" (ADR-002), sem cerimônia extra. */
@Service
class ProdutoService implements CadastrarProdutoUseCase, ConsultarProdutoUseCase, AtualizarProdutoUseCase {

    private final ProdutoRepositoryPort produtos;

    ProdutoService(ProdutoRepositoryPort produtos) {
        this.produtos = produtos;
    }

    @Override
    @Transactional
    public Produto cadastrar(CadastrarProdutoUseCase.Comando comando) {
        var nome = comando.nome().strip();
        var codigoBarras = CodigoBarras.deNullable(comando.codigoBarras());
        if (produtos.existeComNome(nome)) {
            throw nomeDuplicado(nome);
        }
        if (codigoBarras != null && produtos.existeComCodigoBarras(codigoBarras.valor())) {
            throw codigoBarrasDuplicado(codigoBarras);
        }
        int estoqueMinimo = comando.estoqueMinimo() == null ? 0 : comando.estoqueMinimo();
        return produtos.salvar(Produto.novo(comando.nome(), comando.descricao(), comando.preco(), codigoBarras,
                estoqueMinimo));
    }

    @Override
    @Transactional
    public Produto atualizar(long id, AtualizarProdutoUseCase.Comando comando) {
        var produto = buscar(id);
        var nome = comando.nome().strip();
        var codigoBarras = CodigoBarras.deNullable(comando.codigoBarras());
        if (produtos.existeComNomeEmOutroProduto(nome, id)) {
            throw nomeDuplicado(nome);
        }
        if (codigoBarras != null && produtos.existeComCodigoBarrasEmOutroProduto(codigoBarras.valor(), id)) {
            throw codigoBarrasDuplicado(codigoBarras);
        }
        int estoqueMinimo = comando.estoqueMinimo() == null ? produto.estoqueMinimo() : comando.estoqueMinimo();
        produto.atualizar(comando.nome(), comando.descricao(), comando.preco(), codigoBarras, estoqueMinimo);
        return produtos.salvar(produto);
    }

    @Override
    @Transactional(readOnly = true)
    public Produto buscar(long id) {
        return produtos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Produto> listar(FiltroProdutos filtro, Pageable paginacao) {
        return produtos.listar(filtro, paginacao);
    }

    private static RegraNegocioException nomeDuplicado(String nome) {
        return new RegraNegocioException("Já existe um produto com o nome '" + nome + "'");
    }

    private static RegraNegocioException codigoBarrasDuplicado(CodigoBarras codigoBarras) {
        return new RegraNegocioException("Já existe um produto com o código de barras '" + codigoBarras.valor() + "'");
    }
}
