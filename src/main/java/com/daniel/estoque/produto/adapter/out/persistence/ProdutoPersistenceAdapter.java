package com.daniel.estoque.produto.adapter.out.persistence;

import com.daniel.estoque.produto.application.port.in.FiltroProdutos;
import com.daniel.estoque.produto.application.port.out.ProdutoRepositoryPort;
import com.daniel.estoque.produto.domain.Produto;
import com.daniel.estoque.shared.domain.RecursoNaoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
class ProdutoPersistenceAdapter implements ProdutoRepositoryPort {

    private final ProdutoSpringDataRepository repository;

    ProdutoPersistenceAdapter(ProdutoSpringDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Para produtos existentes, atualiza a entidade já carregada (mantém auditoria e @Version)
     * em vez de criar uma nova: assim o lock otimista funciona.
     */
    @Override
    public Produto salvar(Produto produto) {
        var entidade = produto.id() == null
                ? new ProdutoJpaEntity()
                : repository.findById(produto.id())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produto.id()));
        entidade.copiarDe(produto);
        return repository.save(entidade).paraDominio();
    }

    @Override
    public Optional<Produto> buscarPorId(long id) {
        return repository.findById(id).map(ProdutoJpaEntity::paraDominio);
    }

    @Override
    public Page<Produto> listar(FiltroProdutos filtro, Pageable paginacao) {
        return repository.findAll(ProdutoEspecificacoes.de(filtro), paginacao).map(ProdutoJpaEntity::paraDominio);
    }

    @Override
    public List<Produto> buscarPorIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids).stream().map(ProdutoJpaEntity::paraDominio).toList();
    }

    @Override
    public boolean existeComNome(String nome) {
        return repository.existsByNomeIgnoreCase(nome);
    }

    @Override
    public boolean existeComNomeEmOutroProduto(String nome, long id) {
        return repository.existsByNomeIgnoreCaseAndIdNot(nome, id);
    }

    @Override
    public boolean existeComCodigoBarras(String codigoBarras) {
        return repository.existsByCodigoBarras(codigoBarras);
    }

    @Override
    public boolean existeComCodigoBarrasEmOutroProduto(String codigoBarras, long id) {
        return repository.existsByCodigoBarrasAndIdNot(codigoBarras, id);
    }
}
