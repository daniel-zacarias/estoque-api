package com.daniel.estoque.produto.application.port.in;

import com.daniel.estoque.produto.domain.Produto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsultarProdutoUseCase {

    Produto buscar(long id);

    Page<Produto> listar(FiltroProdutos filtro, Pageable paginacao);
}
