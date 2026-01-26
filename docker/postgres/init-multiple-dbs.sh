#!/bin/bash
# =============================================================================
# Script de inicialização do PostgreSQL
# Cria múltiplos databases automaticamente baseado na variável de ambiente
# POSTGRES_MULTIPLE_DATABASES (separados por vírgula)
# =============================================================================

set -e
set -u

# Função para criar um database
function create_database() {
    local database=$1
    echo "=========================================="
    echo "Criando database: $database"
    echo "=========================================="
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $database;
        GRANT ALL PRIVILEGES ON DATABASE $database TO $POSTGRES_USER;
EOSQL
    echo "Database '$database' criado com sucesso!"
}

# Verifica se a variável POSTGRES_MULTIPLE_DATABASES está definida
if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "=========================================="
    echo "Iniciando criação de múltiplos databases"
    echo "=========================================="
    
    # Itera sobre cada database na lista (separados por vírgula)
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_database $db
    done
    
    echo ""
    echo "=========================================="
    echo "Todos os databases foram criados!"
    echo "=========================================="
    echo ""
    
    # Lista os databases criados
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -c "\l"
else
    echo "Variável POSTGRES_MULTIPLE_DATABASES não definida. Nenhum database adicional criado."
fi

