package com.daniel.estoque.shared.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/** Colunas de auditoria comuns. Preenchidas automaticamente pelo Spring Data (ver AuditoriaConfig). */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditavelJpa {

    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private UUID criadoPor;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private UUID atualizadoPor;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    public UUID getCriadoPor() {
        return criadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
