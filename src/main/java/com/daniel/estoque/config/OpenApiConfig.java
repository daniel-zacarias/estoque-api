package com.daniel.estoque.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/**
 * Documentação OpenAPI 3. Swagger UI em /swagger-ui.html, contrato JSON em /v3/api-docs.
 * Para testar pela UI: obtenha um access token no Keycloak e cole em "Authorize".
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Estoque API",
                version = "v1",
                description = """
                        Controle de produtos, saldo de estoque e pedidos.

                        - O saldo só muda por movimentação (ajuste manual ou pedido), nunca por edição direta do produto.
                        - Erros seguem o formato RFC 9457 (`application/problem+json`).
                        - Listagens paginadas usam `pagina`, `tamanho` (máx. 100) e `ordenacao=campo,asc|desc`.
                        """),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
@SecurityScheme(
        name = OpenApiConfig.BEARER_JWT,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Access token emitido pelo Keycloak (realm `estoque`). Roles: ADMIN, OPERADOR.")
class OpenApiConfig {

    static final String BEARER_JWT = "bearer-jwt";

    /** 401 e 403 valem para toda rota protegida; documentar uma vez aqui evita repetir em cada endpoint. */
    @Bean
    OpenApiCustomizer respostasDeSeguranca() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(op -> {
            op.getResponses().addApiResponse("401", problema("Token ausente, inválido ou expirado"));
            op.getResponses().addApiResponse("403", problema("Usuário sem a role exigida pelo endpoint"));
        }));
    }

    private static ApiResponse problema(String descricao) {
        var schema = new Schema<ProblemDetail>().$ref("#/components/schemas/ProblemDetail");
        return new ApiResponse()
                .description(descricao)
                .content(new Content().addMediaType("application/problem+json", new MediaType().schema(schema)));
    }
}
