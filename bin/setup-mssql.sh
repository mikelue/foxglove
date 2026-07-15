#!/usr/bin/env bash

set -euo pipefail

SQL=$(cat <<'SQL'
CREATE DATABASE foxglove_test;
CREATE LOGIN foxglove WITH PASSWORD = 'foxglove_password', CHECK_POLICY = OFF;
ALTER AUTHORIZATION ON DATABASE::foxglove_test TO foxglove;
SQL
)

MSSQL_SA_PASSWORD=dev@146146

printf '%s\n' "$SQL" |
  docker compose -f docker/databases.yaml exec -T mssql \
    /opt/mssql-tools18/bin/sqlcmd \
      -S localhost \
      -U sa \
      -P "${MSSQL_SA_PASSWORD:?MSSQL_SA_PASSWORD must be set}" \
      -C \
      -b
