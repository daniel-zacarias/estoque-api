package com.daniel.estoque.shared.adapter.in.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de resposta paginada. Não serializamos o {@link Page} do Spring direto:
 * o JSON dele não é um contrato estável.
 */
public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

    public PaginaResponse {
        conteudo = List.copyOf(conteudo);
    }

    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
