package com.daniel.estoque.produto.application.port.out;

import com.daniel.estoque.produto.application.port.in.FiltroProdutos;
import com.daniel.estoque.produto.domain.Produto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída. A implementação usa lock otimista (@Version): se dois pedidos
 * baixarem o mesmo produto ao mesmo tempo, um deles falha com conflito em vez de
 * deixar o saldo errado.
 */
public interface ProdutoRepositoryPort {

    Produto salvar(Produto produto);

    Optional<Produto> buscarPorId(long id);

    Page<Produto> listar(FiltroProdutos filtro, Pageable paginacao);

    /** Produtos ativos com esses ids, em qualquer ordem. Ids inexistentes ou excluídos ficam de fora. */
    List<Produto> buscarPorIds(Collection<Long> ids);

    boolean existeComNome(String nome);

    boolean existeComNomeEmOutroProduto(String nome, long id);

    boolean existeComCodigoBarras(String codigoBarras);

    boolean existeComCodigoBarrasEmOutroProduto(String codigoBarras, long id);
}
