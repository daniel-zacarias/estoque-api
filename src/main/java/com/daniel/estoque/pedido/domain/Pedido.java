package com.daniel.estoque.pedido.domain;

import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado Pedido. Controla as transições de status; quem mexe no estoque é o caso de uso,
 * a partir do que este agregado permite.
 */
public final class Pedido {

    private final @Nullable Long id;
    private StatusPedido status;
    private final List<ItemPedido> itens;
    /** Usuário (sub do Keycloak) que criou o pedido. Nulo antes de persistir e em pedidos legados sem autoria. */
    private final @Nullable UUID dono;
    /** Quando foi criado. Nulo antes de persistir. */
    private final @Nullable Instant criadoEm;

    private Pedido(@Nullable Long id, StatusPedido status, List<ItemPedido> itens, @Nullable UUID dono,
                   @Nullable Instant criadoEm) {
        if (itens.isEmpty()) {
            throw new RegraNegocioException("Pedido precisa de ao menos um item");
        }
        var produtos = new HashSet<Long>();
        for (var item : itens) {
            if (!produtos.add(item.produtoId())) {
                throw new RegraNegocioException("Produto " + item.produtoId() + " repetido no pedido");
            }
        }
        this.id = id;
        this.status = status;
        this.itens = List.copyOf(itens);
        this.dono = dono;
        this.criadoEm = criadoEm;
    }

    /** O dono é preenchido pela persistência (auditoria), a partir do usuário autenticado. */
    public static Pedido criar(List<ItemPedido> itens) {
        return new Pedido(null, StatusPedido.ABERTO, itens, null, null);
    }

    public static Pedido reconstituir(Long id, StatusPedido status, List<ItemPedido> itens, @Nullable UUID dono,
                                      @Nullable Instant criadoEm) {
        return new Pedido(Objects.requireNonNull(id), status, itens, dono, criadoEm);
    }

    public static Pedido reconstituir(Long id, StatusPedido status, List<ItemPedido> itens, @Nullable UUID dono) {
        return reconstituir(id, status, itens, dono, null);
    }

    /** Pedido sem dono conhecido (legado) não pertence a ninguém: só quem vê todos os pedidos acessa. */
    public boolean pertenceA(UUID usuario) {
        return usuario.equals(dono);
    }

    public @Nullable UUID dono() {
        return dono;
    }

    public @Nullable Instant criadoEm() {
        return criadoEm;
    }

    public void confirmar() {
        if (status != StatusPedido.ABERTO) {
            throw new RegraNegocioException("Só pedidos ABERTOS podem ser confirmados (atual: " + status + ")");
        }
        status = StatusPedido.CONFIRMADO;
    }

    /**
     * Cancela o pedido.
     *
     * @return {@code true} se o estoque precisa ser devolvido (o pedido estava confirmado)
     */
    public boolean cancelar() {
        boolean exigeEstorno = switch (status) {
            case ABERTO -> false;
            case CONFIRMADO -> true;
            case CANCELADO -> throw new RegraNegocioException("Pedido já está cancelado");
        };
        status = StatusPedido.CANCELADO;
        return exigeEstorno;
    }

    public BigDecimal total() {
        return itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Itens em ordem de produto. Processar sempre na mesma ordem evita deadlock quando
     * dois pedidos com os mesmos produtos são confirmados ao mesmo tempo.
     */
    public List<ItemPedido> itensEmOrdemDeProduto() {
        return itens.stream().sorted(Comparator.comparingLong(ItemPedido::produtoId)).toList();
    }

    public @Nullable Long id() {
        return id;
    }

    public long idPersistido() {
        return Objects.requireNonNull(id, "Pedido ainda não foi persistido");
    }

    public StatusPedido status() {
        return status;
    }

    public List<ItemPedido> itens() {
        return itens;
    }
}
