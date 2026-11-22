package com.daniel.estoque.produto.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodigoBarrasTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "96385074",         // GTIN-8
            "036000291452",     // GTIN-12 / UPC-A
            "7891000315507",    // GTIN-13 / EAN-13
            "17891000315504"    // GTIN-14
    })
    void aceitaGtinValido(String valor) {
        assertThat(new CodigoBarras(valor).valor()).isEqualTo(valor);
    }

    @Test
    void removeEspacosDasPontas() {
        assertThat(new CodigoBarras(" 7891000315507 ").valor()).isEqualTo("7891000315507");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "7891000315508",    // dígito verificador errado
            "789100031550A",    // letra
            "1234567",          // tamanho inválido
            "12345678901",      // 11 dígitos
            "123456789012345"   // 15 dígitos
    })
    void rejeitaCodigoInvalido(String valor) {
        assertThatThrownBy(() -> new CodigoBarras(valor)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void nuloOuEmBrancoViraAusencia() {
        assertThat(CodigoBarras.deNullable(null)).isNull();
        assertThat(CodigoBarras.deNullable("   ")).isNull();
        assertThat(CodigoBarras.deNullable("7891000315507")).isEqualTo(new CodigoBarras("7891000315507"));
    }
}
