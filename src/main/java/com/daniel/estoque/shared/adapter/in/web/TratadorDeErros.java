package com.daniel.estoque.shared.adapter.in.web;

import com.daniel.estoque.shared.domain.RecursoNaoEncontradoException;
import com.daniel.estoque.shared.domain.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz exceções do domínio para respostas HTTP no formato RFC 9457 (ProblemDetail).
 * O @ResponseStatus de cada handler também alimenta a documentação OpenAPI.
 */
@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ProblemDetail regraDeNegocio(RegraNegocioException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(Ordenacao.OrdenacaoInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail ordenacaoInvalida(Ordenacao.OrdenacaoInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Constraint do banco violada (ex.: dois cadastros simultâneos com o mesmo nome passam pela checagem
     * do serviço e só o índice único barra). Não devolve a mensagem do banco: ela expõe tabelas e colunas.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail violacaoDeIntegridade(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A operação conflita com dados já existentes (registro duplicado ou referência inválida).");
    }

    /** Duas transações alteraram o mesmo produto ao mesmo tempo (lock otimista). O cliente pode repetir. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail conflito(ObjectOptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "O registro foi alterado por outra operação. Tente novamente.");
    }
}
