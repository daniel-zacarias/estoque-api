package com.daniel.estoque.config;

import com.daniel.estoque.estoque.application.port.in.AjustarEstoqueUseCase;
import com.daniel.estoque.estoque.application.port.in.ConsultarMovimentacoesUseCase;
import com.daniel.estoque.pedido.application.port.in.AlterarStatusPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.ConsultarPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.CriarPedidoUseCase;
import com.daniel.estoque.produto.application.port.in.AtualizarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.CadastrarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.ConsultarProdutoUseCase;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocPageableConfiguration;
import org.springdoc.core.configuration.SpringDocSortConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que o contrato OpenAPI é gerado, fica acessível sem token e documenta o essencial.
 * Fatia web apenas: casos de uso mockados, sem banco e sem Keycloak.
 */
@WebMvcTest
@Import({SecurityConfig.class, OpenApiConfig.class})
@ImportAutoConfiguration({
        // Como na aplicação real: proxy de classe (CGLIB). Sem isso, o @PreAuthorize de um controller que
        // implementa *ControllerDoc vira proxy JDK da interface e os @RequestMapping somem.
        AopAutoConfiguration.class,
        SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocSpecPropertiesConfiguration.class,
        SpringDocPageableConfiguration.class, SpringDocSortConfiguration.class, SpringDocWebMvcConfiguration.class,
        SwaggerConfig.class, SwaggerUiConfigProperties.class, SwaggerUiOAuthProperties.class})
class DocumentacaoOpenApiTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean CadastrarProdutoUseCase cadastrarProduto;
    @MockitoBean ConsultarProdutoUseCase consultarProduto;
    @MockitoBean AtualizarProdutoUseCase atualizarProduto;
    @MockitoBean AjustarEstoqueUseCase ajustarEstoque;
    @MockitoBean ConsultarMovimentacoesUseCase consultarMovimentacoes;
    @MockitoBean CriarPedidoUseCase criarPedido;
    @MockitoBean AlterarStatusPedidoUseCase alterarStatusPedido;
    @MockitoBean ConsultarPedidoUseCase consultarPedido;

    @Test
    void contratoEPublicoEDocumentaAsOperacoes() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Estoque API"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/produtos'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{produtoId}/ajustes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pedidos/{id}/cancelar'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pedidos'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/movimentacoes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/movimentacoes/totais'].get").exists())
                .andExpect(jsonPath("$.components.schemas.Produto.properties.estoqueMinimo").exists());
    }

    /** As anotações ficam nas interfaces *ControllerDoc; o springdoc precisa enxergá-las pelo controller. */
    @Test
    void leAsAnotacoesDasInterfacesDeDocumentacao() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.tags[*].name", hasItems("Produtos", "Estoque", "Pedidos")))
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.tags[0]").value("Produtos"))
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.summary")
                        .value("Atualiza os dados cadastrais de um produto"))
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.parameters[0].description")
                        .value("Id do produto"))
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.responses['200'].description")
                        .value("Produto atualizado"))
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{produtoId}/ajustes'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pedidos/{id}/cancelar'].post.summary").value("Cancela um pedido"));
    }

    @Test
    void documentaRespostasDeErro() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.responses['422']").exists());
    }

    @Test
    void usaOsNomesDeParametroDePaginacaoConfigurados() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/produtos'].get.parameters[*].name",
                        hasItems("pagina", "tamanho", "ordenacao")));
    }

    @Test
    void documentaOCodigoDeBarras() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.CadastrarProdutoRequest.properties.codigoBarras.example")
                        .value("7891000315507"))
                .andExpect(jsonPath("$.components.schemas.Produto.properties.codigoBarras").exists());
    }

    @Test
    void swaggerUiEPublicoMasAApiNao() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/produtos")).andExpect(status().isUnauthorized());
    }
}
