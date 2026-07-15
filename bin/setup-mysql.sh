#!/usr/bin/env bash

set -euo pipefail

SQL=$(cat <<'SQL'
CREATE DATABASE foxglove_test;
CREATE USER 'foxglove'@'%' IDENTIFIED BY 'foxglove_password';
GRANT ALL PRIVILEGES ON foxglove_test.* TO 'foxglove'@'%';
FLUSH PRIVILEGES;
SQL
)

MYSQL_ROOT_PASSWORD=dev@146

printf '%s\n' "$SQL" |
  docker compose -f docker/databases.yaml exec -T mysql \
    mysql \
      -h localhost \
      -u root \
      -p"${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD must be set}"
