package com.daniel.estoque.pedido.application.port.in;

import java.util.Objects;
import java.util.UUID;

/**
 * Quem está pedindo a operação. O adaptador web monta a partir do token; a aplicação decide o acesso
 * sem conhecer Spring Security.
 *
 * @param usuario      "sub" do token (UUID do usuário no Keycloak)
 * @param veTodosOsPedidos ADMIN: vê e altera qualquer pedido. Os demais só os que criaram.
 */
public record Solicitante(UUID usuario, boolean veTodosOsPedidos) {

    public Solicitante {
        Objects.requireNonNull(usuario, "usuario");
    }
}
