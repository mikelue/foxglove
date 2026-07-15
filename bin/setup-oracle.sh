#!/usr/bin/env bash

set -euo pipefail

SQL=$(cat <<'SQL'
CREATE USER foxglove IDENTIFIED BY foxglove_password;
GRANT CONNECT, RESOURCE TO foxglove;
ALTER USER foxglove QUOTA UNLIMITED ON USERS;
EXIT;
SQL
)

ORACLE_PASSWORD=dev@146
ORACLE_SERVICE=FREEPDB1

printf '%s\n' "$SQL" |
  docker compose -f docker/databases.yaml exec -T oracle \
    sqlplus -s \
      "system/\"${ORACLE_PASSWORD}\"@//localhost:1521/${ORACLE_SERVICE}"
