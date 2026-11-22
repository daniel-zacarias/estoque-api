package com.daniel.estoque.produto.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoEspecificacoesTest {

    /** Sem escapar, "%" na busca casaria com qualquer nome. */
    @Test
    void buscaTrataCuringasDoLikeComoTexto() {
        assertThat(ProdutoEspecificacoes.escaparLike("50% off_x\\y")).isEqualTo("50\\% off\\_x\\\\y");
    }
}
