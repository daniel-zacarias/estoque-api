package com.daniel.estoque.produto.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Produto e seu saldo atual. Toda alteração de saldo passa por {@link #baixar} ou {@link #repor},
 * que garantem a invariante: o estoque nunca fica negativo.
 */
public final class Produto {

    private final @Nullable Long id;
    private String nome;
    private @Nullable String descricao;
    private BigDecimal preco;
    private @Nullable CodigoBarras codigoBarras;
    private int qtEstoque;
    /** Abaixo dele o produto aparece como "saldo baixo". Zero desliga o alerta. */
    private int estoqueMinimo;

    private Produto(@Nullable Long id, String nome, @Nullable String descricao, BigDecimal preco,
                    @Nullable CodigoBarras codigoBarras, int qtEstoque, int estoqueMinimo) {
        if (qtEstoque < 0) {
            throw new RegraNegocioException("Estoque não pode ser negativo");
        }
        this.id = id;
        this.nome = validarNome(nome);
        this.descricao = descricao;
        this.preco = validarPreco(preco);
        this.codigoBarras = codigoBarras;
        this.qtEstoque = qtEstoque;
        this.estoqueMinimo = validarEstoqueMinimo(estoqueMinimo);
    }

    /** Produto novo sempre nasce com estoque zero: a entrada é registrada como movimentação. */
    public static Produto novo(String nome, @Nullable String descricao, BigDecimal preco,
                               @Nullable CodigoBarras codigoBarras, int estoqueMinimo) {
        return new Produto(null, nome, descricao, preco, codigoBarras, 0, estoqueMinimo);
    }

    /** Sem estoque mínimo (alerta de saldo baixo desligado). */
    public static Produto novo(String nome, @Nullable String descricao, BigDecimal preco,
                               @Nullable CodigoBarras codigoBarras) {
        return novo(nome, descricao, preco, codigoBarras, 0);
    }

    /** Usado pelo adaptador de persistência para remontar um produto já salvo. */
    public static Produto reconstituir(Long id, String nome, @Nullable String descricao, BigDecimal preco,
                                       int qtEstoque, @Nullable CodigoBarras codigoBarras, int estoqueMinimo) {
        return new Produto(Objects.requireNonNull(id), nome, descricao, preco, codigoBarras, qtEstoque, estoqueMinimo);
    }

    public static Produto reconstituir(Long id, String nome, @Nullable String descricao, BigDecimal preco,
                                       int qtEstoque, @Nullable CodigoBarras codigoBarras) {
        return reconstituir(id, nome, descricao, preco, qtEstoque, codigoBarras, 0);
    }

    /**
     * Atualiza os dados cadastrais. O saldo não muda aqui: só por {@link #baixar}/{@link #repor},
     * para que toda alteração de estoque fique registrada como movimentação.
     */
    public void atualizar(String nome, @Nullable String descricao, BigDecimal preco,
                          @Nullable CodigoBarras codigoBarras) {
        var nomeValidado = validarNome(nome);
        var precoValidado = validarPreco(preco);
        this.nome = nomeValidado;
        this.descricao = descricao;
        this.preco = precoValidado;
        this.codigoBarras = codigoBarras;
    }

    /** Igual ao anterior, trocando também o estoque mínimo. Valida tudo antes de alterar qualquer campo. */
    public void atualizar(String nome, @Nullable String descricao, BigDecimal preco,
                          @Nullable CodigoBarras codigoBarras, int estoqueMinimo) {
        var minimoValidado = validarEstoqueMinimo(estoqueMinimo);
        atualizar(nome, descricao, preco, codigoBarras);
        this.estoqueMinimo = minimoValidado;
    }

    public SituacaoEstoque situacao() {
        return SituacaoEstoque.de(qtEstoque, estoqueMinimo);
    }

    public void baixar(int quantidade) {
        exigirPositiva(quantidade);
        if (quantidade > qtEstoque) {
            throw new EstoqueInsuficienteException(nome, qtEstoque, quantidade);
        }
        qtEstoque -= quantidade;
    }

    public void repor(int quantidade) {
        exigirPositiva(quantidade);
        if (quantidade > Integer.MAX_VALUE - qtEstoque) {
            throw new RegraNegocioException("Saldo excederia o máximo permitido (" + Integer.MAX_VALUE + ")");
        }
        qtEstoque += quantidade;
    }

    private static String validarNome(String nome) {
        if (nome.isBlank()) {
            throw new RegraNegocioException("Nome do produto é obrigatório");
        }
        return nome.strip();
    }

    private static BigDecimal validarPreco(BigDecimal preco) {
        if (preco.signum() < 0) {
            throw new RegraNegocioException("Preço não pode ser negativo");
        }
        return preco;
    }

    private static int validarEstoqueMinimo(int estoqueMinimo) {
        if (estoqueMinimo < 0) {
            throw new RegraNegocioException("Estoque mínimo não pode ser negativo");
        }
        return estoqueMinimo;
    }

    private static void exigirPositiva(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("Quantidade deve ser maior que zero");
        }
    }

    public @Nullable Long id() {
        return id;
    }

    /** Id de um produto já persistido. Falha se chamado num produto novo. */
    public long idPersistido() {
        return Objects.requireNonNull(id, "Produto ainda não foi persistido");
    }

    public String nome() {
        return nome;
    }

    public @Nullable String descricao() {
        return descricao;
    }

    public BigDecimal preco() {
        return preco;
    }

    public @Nullable CodigoBarras codigoBarras() {
        return codigoBarras;
    }

    public int qtEstoque() {
        return qtEstoque;
    }

    public int estoqueMinimo() {
        return estoqueMinimo;
    }
}
