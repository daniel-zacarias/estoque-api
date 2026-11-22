-- Código de barras (GTIN-8, GTIN-12/UPC-A, GTIN-13/EAN-13 ou GTIN-14), opcional.
-- O dígito verificador é validado no domínio (CodigoBarras); aqui só o formato.

ALTER TABLE produto ADD COLUMN codigo_barras VARCHAR(14)
    CONSTRAINT ck_produto_codigo_barras CHECK (codigo_barras ~ '^([0-9]{8}|[0-9]{12,14})$');

-- Único só entre produtos ativos, mesmo padrão de uk_produto_nome_ativo
CREATE UNIQUE INDEX uk_produto_codigo_barras_ativo ON produto (codigo_barras)
    WHERE deletado_em IS NULL AND codigo_barras IS NOT NULL;
