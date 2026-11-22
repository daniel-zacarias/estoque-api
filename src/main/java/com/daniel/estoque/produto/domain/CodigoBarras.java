package com.daniel.estoque.produto.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.jspecify.annotations.Nullable;

/**
 * Código de barras GTIN (GTIN-8, GTIN-12/UPC-A, GTIN-13/EAN-13 ou GTIN-14).
 * Só existe se for válido: formato e dígito verificador são conferidos na criação.
 */
public record CodigoBarras(String valor) {

    public CodigoBarras {
        valor = valor.strip();
        if (!valor.matches("\\d{8}|\\d{12,14}")) {
            throw new RegraNegocioException(
                    "Código de barras inválido: '" + valor + "' (esperado GTIN com 8, 12, 13 ou 14 dígitos)");
        }
        if (!digitoVerificadorConfere(valor)) {
            throw new RegraNegocioException("Código de barras inválido: dígito verificador não confere em '" + valor + "'");
        }
    }

    /** Null ou em branco vira ausência de código; qualquer outro valor precisa ser um GTIN válido. */
    public static @Nullable CodigoBarras deNullable(@Nullable String valor) {
        return valor == null || valor.isBlank() ? null : new CodigoBarras(valor);
    }

    /** Módulo 10 do GTIN: pesos 3 e 1 alternados, começando por 3 no dígito mais à direita (sem o verificador). */
    private static boolean digitoVerificadorConfere(String digitos) {
        int ultimo = digitos.length() - 1;
        int soma = 0;
        for (int i = ultimo - 1, peso = 3; i >= 0; i--, peso = 4 - peso) {
            soma += (digitos.charAt(i) - '0') * peso;
        }
        int esperado = (10 - soma % 10) % 10;
        return digitos.charAt(ultimo) - '0' == esperado;
    }
}
