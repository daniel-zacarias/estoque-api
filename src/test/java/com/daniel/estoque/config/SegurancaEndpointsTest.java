package com.daniel.estoque.config;

import com.daniel.estoque.estoque.application.port.in.AjustarEstoqueUseCase;
import com.daniel.estoque.estoque.application.port.in.ConsultarMovimentacoesUseCase;
import com.daniel.estoque.estoque.application.port.in.FiltroMovimentacoes;
import com.daniel.estoque.estoque.application.port.in.TotaisMovimentacao;
import com.daniel.estoque.pedido.application.port.in.AlterarStatusPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.ConsultarPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.CriarPedidoUseCase;
import com.daniel.estoque.pedido.application.port.in.Solicitante;
import com.daniel.estoque.pedido.domain.ItemPedido;
import com.daniel.estoque.pedido.domain.Pedido;
import com.daniel.estoque.pedido.domain.StatusPedido;
import com.daniel.estoque.shared.domain.RecursoNaoEncontradoException;
import com.daniel.estoque.produto.application.port.in.AtualizarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.CadastrarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.ConsultarProdutoUseCase;
import com.daniel.estoque.produto.application.port.in.FiltroProdutos;
import com.daniel.estoque.produto.domain.SituacaoEstoque;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autorização: quem pode chamar o quê. Regressão aqui é falha de segurança,
 * então cada regra de role tem um teste. Fatia web apenas, casos de uso mockados.
 */
@WebMvcTest
@Import(SecurityConfig.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class SegurancaEndpointsTest {

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

    /** Cada chamada usa um "sub" novo para não somar no limite de requisições entre testes. */
    private static JwtRequestPostProcessor usuario(String... roles) {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(java.util.Arrays.stream(roles)
                        .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mvc.perform(get("/api/v1/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/pedidos/1/confirmar")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSemRoleRecebe403() throws Exception {
        mvc.perform(get("/api/v1/produtos").with(usuario())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pedidos/1").with(usuario())).andExpect(status().isForbidden());
    }

    @Test
    void operadorNaoCadastraNemEditaProduto() throws Exception {
        mvc.perform(post("/api/v1/produtos").with(usuario("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"preco\":1.00}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operadorNaoAjustaEstoque() throws Exception {
        mvc.perform(post("/api/v1/produtos/1/ajustes").with(usuario("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"motivo\":\"REPOSICAO\",\"quantidade\":1,\"observacao\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operadorConsultaProdutos() throws Exception {
        given(consultarProduto.listar(any(), any())).willReturn(Page.empty());
        mvc.perform(get("/api/v1/produtos").with(usuario("OPERADOR"))).andExpect(status().isOk());
    }

    @Test
    void ordenacaoPorCampoInternoERecusada() throws Exception {
        mvc.perform(get("/api/v1/produtos").param("ordenacao", "criadoPor,asc").with(usuario("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pedidoComItensDemaisERecusado() throws Exception {
        var itens = IntStream.rangeClosed(1, 101)
                .mapToObj(i -> "{\"produtoId\":" + i + ",\"quantidade\":1}")
                .collect(Collectors.joining(",", "[", "]"));
        mvc.perform(post("/api/v1/pedidos").with(usuario("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":" + itens + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void operadorChegaAoServicoComoeleMesmoSemVerTodos() throws Exception {
        var sub = UUID.randomUUID();
        given(consultarPedido.buscar(eq(1L), any())).willReturn(pedidoQualquer());
        mvc.perform(get("/api/v1/pedidos/1").with(jwt().jwt(j -> j.subject(sub.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR"))))
                .andExpect(status().isOk());
        verify(consultarPedido).buscar(1L, new Solicitante(sub, false));
    }

    @Test
    void adminChegaAoServicoVendoTodos() throws Exception {
        var sub = UUID.randomUUID();
        given(alterarStatusPedido.cancelar(eq(1L), any())).willReturn(pedidoQualquer());
        mvc.perform(post("/api/v1/pedidos/1/cancelar").with(jwt().jwt(j -> j.subject(sub.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        verify(alterarStatusPedido).cancelar(1L, new Solicitante(sub, true));
    }

    @Test
    void pedidoDeOutroUsuarioResponde404() throws Exception {
        given(alterarStatusPedido.confirmar(eq(1L), any())).willThrow(new RecursoNaoEncontradoException("Pedido", 1L));
        mvc.perform(post("/api/v1/pedidos/1/confirmar").with(usuario("OPERADOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void operadorListaPedidosComoEleMesmoSemVerTodos() throws Exception {
        var sub = UUID.randomUUID();
        given(consultarPedido.listar(any(), any(), any())).willReturn(Page.empty());
        mvc.perform(get("/api/v1/pedidos").param("status", "ABERTO").with(jwt().jwt(j -> j.subject(sub.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR"))))
                .andExpect(status().isOk());
        verify(consultarPedido).listar(eq(StatusPedido.ABERTO), eq(new Solicitante(sub, false)), any());
    }

    @Test
    void adminListaPedidosVendoTodos() throws Exception {
        var sub = UUID.randomUUID();
        given(consultarPedido.listar(any(), any(), any())).willReturn(Page.empty());
        mvc.perform(get("/api/v1/pedidos").with(jwt().jwt(j -> j.subject(sub.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        verify(consultarPedido).listar(eq(null), eq(new Solicitante(sub, true)), any());
    }

    @Test
    void listagemDePedidosRecusaOrdenacaoPorCampoInterno() throws Exception {
        mvc.perform(get("/api/v1/pedidos").param("ordenacao", "criadoPor,asc").with(usuario("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historicoGeralExigeRole() throws Exception {
        mvc.perform(get("/api/v1/movimentacoes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/movimentacoes").with(usuario())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/movimentacoes/totais").with(usuario())).andExpect(status().isForbidden());
    }

    @Test
    void operadorConsultaHistoricoGeralETotais() throws Exception {
        given(consultarMovimentacoes.listar(any(), any())).willReturn(Page.empty());
        given(consultarMovimentacoes.totais(any(), any())).willReturn(TotaisMovimentacao.ZERO);
        mvc.perform(get("/api/v1/movimentacoes").param("tipo", "SAIDA").param("de", "2026-11-12")
                        .param("ate", "2026-11-19").with(usuario("OPERADOR")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/movimentacoes/totais").with(usuario("OPERADOR")))
                .andExpect(status().isOk());
    }

    /** "ate" é inclusivo: o filtro vai até o começo do dia seguinte, no fuso de São Paulo (UTC-3). */
    @Test
    void periodoDoHistoricoUsaDiasDeSaoPaulo() throws Exception {
        given(consultarMovimentacoes.listar(any(), any())).willReturn(Page.empty());
        mvc.perform(get("/api/v1/movimentacoes").param("de", "2026-11-19").param("ate", "2026-11-19")
                        .with(usuario("ADMIN")))
                .andExpect(status().isOk());
        verify(consultarMovimentacoes).listar(eq(new FiltroMovimentacoes(null, null,
                Instant.parse("2026-11-19T03:00:00Z"), Instant.parse("2026-11-20T03:00:00Z"))), any());
    }

    @Test
    void filtroDeSituacaoInvalidoRecebe400() throws Exception {
        mvc.perform(get("/api/v1/produtos").param("situacao", "QUALQUER").with(usuario("OPERADOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void operadorFiltraProdutosPorSituacao() throws Exception {
        given(consultarProduto.listar(any(), any())).willReturn(Page.empty());
        mvc.perform(get("/api/v1/produtos").param("situacao", "SALDO_BAIXO").param("busca", " parafuso ")
                        .with(usuario("OPERADOR")))
                .andExpect(status().isOk());
        verify(consultarProduto).listar(eq(new FiltroProdutos("parafuso", SituacaoEstoque.SALDO_BAIXO)), any());
    }

    private static Pedido pedidoQualquer() {
        return Pedido.reconstituir(1L, StatusPedido.ABERTO,
                List.of(new ItemPedido(1L, 1, java.math.BigDecimal.ONE)), UUID.randomUUID());
    }

    @Test
    void conversorLeAsRolesDoRealmDoKeycloak() {
        var token = Jwt.withTokenValue("t").header("alg", "RS256")
                .claim("realm_access", Map.of("roles", List.of("admin", "operador")))
                .build();
        var authorities = new SecurityConfig.KeycloakRealmRolesConverter().convert(token);
        assertThat(authorities).extracting(Object::toString).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_OPERADOR");
    }

    @Test
    void conversorSemRealmAccessNaoDaNenhumaRole() {
        var token = Jwt.withTokenValue("t").header("alg", "RS256").claim("sub", "x").build();
        assertThat(new SecurityConfig.KeycloakRealmRolesConverter().convert(token)).isEmpty();
    }
}
