package com.daniel.estoque.produto.application.port.in;

import com.daniel.estoque.produto.domain.Produto;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/** Atualiza os dados cadastrais do produto. Saldo de estoque não entra: muda só por movimentação. */
public interface AtualizarProdutoUseCase {

    Produto atualizar(long id, Comando comando);

    /** @param estoqueMinimo nulo = mantém o atual (clientes antigos não zeram o mínimo sem querer) */
    record Comando(String nome, @Nullable String descricao, BigDecimal preco, @Nullable String codigoBarras,
                   @Nullable Integer estoqueMinimo) {

        public Comando(String nome, @Nullable String descricao, BigDecimal preco, @Nullable String codigoBarras) {
            this(nome, descricao, preco, codigoBarras, null);
        }
    }
}
