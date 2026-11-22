package com.daniel.estoque.pedido.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;

import java.math.BigDecimal;

/**
 * Linha do pedido. O {@code valorUnitario} é o preço no momento do pedido:
 * se o preço do produto mudar depois, o pedido não muda junto.
 */
public record ItemPedido(long produtoId, int quantidade, BigDecimal valorUnitario) {

    public ItemPedido {
        if (quantidade <= 0) {
            throw new RegraNegocioException("Quantidade do item deve ser maior que zero");
        }
        if (valorUnitario.signum() < 0) {
            throw new RegraNegocioException("Valor unitário não pode ser negativo");
        }
    }

    public BigDecimal subtotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
