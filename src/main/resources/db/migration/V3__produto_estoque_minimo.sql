-- Estoque mínimo por produto: abaixo dele o produto aparece como "saldo baixo" no app.
-- Zero (padrão) desliga o alerta, então os produtos existentes continuam como estavam.

ALTER TABLE produto ADD COLUMN estoque_minimo INTEGER NOT NULL DEFAULT 0
    CONSTRAINT ck_produto_estoque_minimo CHECK (estoque_minimo >= 0);

-- Filtros "saldo baixo" e "sem estoque" da listagem (só produtos ativos)
CREATE INDEX idx_produto_saldo_ativo ON produto (qt_estoque, estoque_minimo) WHERE deletado_em IS NULL;
