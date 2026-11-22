package com.daniel.estoque.estoque.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.jspecify.annotations.Nullable;

/**
 * Registro imutável de uma mudança de saldo. Nunca é editado nem apagado:
 * um erro se corrige com uma movimentação inversa.
 * Quem e quando ({@code criado_por}, {@code criado_em}) são preenchidos pela auditoria da persistência.
 */
public record MovimentacaoEstoque(
        long produtoId,
        @Nullable Long pedidoId,
        TipoMovimento tipo,
        MotivoMovimento motivo,
        int quantidade,
        int saldoAnterior,
        int saldoPosterior,
        @Nullable String observacao) {

    public MovimentacaoEstoque {
        if (quantidade <= 0) {
            throw new RegraNegocioException("Quantidade da movimentação deve ser maior que zero");
        }
        int esperado = switch (tipo) {
            case ENTRADA -> saldoAnterior + quantidade;
            case SAIDA -> saldoAnterior - quantidade;
        };
        if (saldoPosterior != esperado) {
            throw new RegraNegocioException("Saldo posterior inconsistente com a movimentação");
        }
        if (motivo.permitidoEmAjusteManual() && (observacao == null || observacao.isBlank())) {
            throw new RegraNegocioException("Ajuste manual exige observação");
        }
    }

    public static MovimentacaoEstoque saidaPorPedido(long produtoId, long pedidoId, int quantidade, int saldoAnterior) {
        return new MovimentacaoEstoque(produtoId, pedidoId, TipoMovimento.SAIDA, MotivoMovimento.PEDIDO,
                quantidade, saldoAnterior, saldoAnterior - quantidade, null);
    }

    public static MovimentacaoEstoque estornoDePedido(long produtoId, long pedidoId, int quantidade, int saldoAnterior) {
        return new MovimentacaoEstoque(produtoId, pedidoId, TipoMovimento.ENTRADA, MotivoMovimento.CANCELAMENTO_PEDIDO,
                quantidade, saldoAnterior, saldoAnterior + quantidade, null);
    }

    public static MovimentacaoEstoque ajusteManual(long produtoId, TipoMovimento tipo, MotivoMovimento motivo,
                                                   int quantidade, int saldoAnterior, String observacao) {
        if (!motivo.permitidoEmAjusteManual()) {
            throw new RegraNegocioException("Motivo " + motivo + " não pode ser lançado manualmente");
        }
        int saldoPosterior = tipo == TipoMovimento.ENTRADA ? saldoAnterior + quantidade : saldoAnterior - quantidade;
        return new MovimentacaoEstoque(produtoId, null, tipo, motivo, quantidade, saldoAnterior, saldoPosterior, observacao);
    }
}
