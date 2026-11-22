#!/bin/sh
# Roda só na primeira inicialização do volume (ADR-001). Cria:
#   - keycloak          : dono do database do Keycloak
#   - estoque_migracao  : dono do database "estoque" (Flyway: DDL), sem superusuário
#   - estoque_app       : usado pela API em runtime, só DML nas tabelas
# O POSTGRES_USER (superusuário) fica só para administração, nunca na aplicação.
#
# As senhas entram como variáveis do psql (:'var'), que fazem o escape correto:
# uma senha com aspas não quebra o SQL.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v senha_keycloak="$KEYCLOAK_DB_PASSWORD" \
     -v senha_migracao="$MIGRACAO_DB_PASSWORD" \
     -v senha_app="$API_DB_PASSWORD" \
     -v banco="$POSTGRES_DB" <<-'EOSQL'
	CREATE USER keycloak WITH PASSWORD :'senha_keycloak';
	CREATE DATABASE keycloak OWNER keycloak;
	REVOKE ALL ON DATABASE keycloak FROM PUBLIC;

	CREATE USER estoque_migracao WITH PASSWORD :'senha_migracao';
	CREATE USER estoque_app WITH PASSWORD :'senha_app';

	-- Dono do database = dono do schema public (pg_database_owner, PG 15+)
	ALTER DATABASE :"banco" OWNER TO estoque_migracao;
	REVOKE ALL ON DATABASE :"banco" FROM PUBLIC;
	GRANT CONNECT ON DATABASE :"banco" TO estoque_app;
	GRANT USAGE ON SCHEMA public TO estoque_app;

	-- Tudo que o Flyway criar já nasce com DML liberado para a API, e nada além disso
	ALTER DEFAULT PRIVILEGES FOR ROLE estoque_migracao IN SCHEMA public
	    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO estoque_app;
	ALTER DEFAULT PRIVILEGES FOR ROLE estoque_migracao IN SCHEMA public
	    GRANT USAGE, SELECT ON SEQUENCES TO estoque_app;
EOSQL
