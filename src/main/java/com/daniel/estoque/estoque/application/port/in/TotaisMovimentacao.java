package com.daniel.estoque.estoque.application.port.in;

/**
 * Soma das movimentações de um período.
 *
 * @param entradas        quantas movimentações de ENTRADA
 * @param unidadesEntrada soma das quantidades de ENTRADA
 * @param saidas          quantas movimentações de SAIDA
 * @param unidadesSaida   soma das quantidades de SAIDA
 */
public record TotaisMovimentacao(long entradas, long unidadesEntrada, long saidas, long unidadesSaida) {

    public static final TotaisMovimentacao ZERO = new TotaisMovimentacao(0, 0, 0, 0);
}
