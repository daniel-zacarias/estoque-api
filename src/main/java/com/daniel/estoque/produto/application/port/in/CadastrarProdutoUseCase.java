package com.daniel.estoque.produto.application.port.in;

import com.daniel.estoque.produto.domain.Produto;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public interface CadastrarProdutoUseCase {

    Produto cadastrar(Comando comando);

    /** @param estoqueMinimo nulo = 0 (sem alerta de saldo baixo) */
    record Comando(String nome, @Nullable String descricao, BigDecimal preco, @Nullable String codigoBarras,
                   @Nullable Integer estoqueMinimo) {

        public Comando(String nome, @Nullable String descricao, BigDecimal preco, @Nullable String codigoBarras) {
            this(nome, descricao, preco, codigoBarras, null);
        }
    }
}
