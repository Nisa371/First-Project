#!/usr/bin/env bash
# Local Linux bootstrap. Admin authentication never becomes application authentication.
# Default: sudo mysql (socket auth); use --password-admin for mysql -u root -p.
# Re-running preserves tables/data and synchronizes the app user's password with .env.
set -euo pipefail
backend_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$backend_dir/load-env.sh"
load_backend_env "$backend_dir/.env"
command -v mysql >/dev/null || { echo 'Install MySQL first: sudo apt install mysql-server mysql-client' >&2; exit 1; }
[[ -z "${DB_URL:-}" ]] || { echo 'Unset DB_URL for setup; configure DB_HOST/DB_PORT/DB_NAME instead.' >&2; exit 1; }
[[ "$DB_HOST" == localhost || "$DB_HOST" == 127.0.0.1 ]] && [[ "$DB_PORT" == 3306 ]] || {
    echo 'This helper sets up local MySQL on port 3306. Provision remote/custom-port servers manually.' >&2; exit 1;
}
[[ "$DB_NAME" =~ ^[A-Za-z0-9_]{1,64}$ && "$DB_USERNAME" =~ ^[A-Za-z0-9_]{1,32}$ ]] || {
    echo 'Use only letters, digits and underscores for DB_NAME (1–64) and DB_USERNAME (1–32).' >&2; exit 1;
}
case "${DB_NAME,,}" in
    mysql|sys|information_schema|performance_schema)
        echo 'DB_NAME must name an application database, not a MySQL system database.' >&2; exit 1 ;;
esac
case "${1:-}" in
    '')
        if (( EUID == 0 )); then admin=(mysql --protocol=socket -u root)
        else
            command -v sudo >/dev/null || { echo 'Install sudo or use ./setup-db.sh --password-admin.' >&2; exit 1; }
            sudo -v || { echo 'Administrator access is required to prepare MySQL.' >&2; exit 1; }
            admin=(sudo mysql --protocol=socket -u root)
        fi ;;
    --password-admin) admin=(mysql --protocol=socket -u root -p) ;;
    *) echo 'Usage: ./setup-db.sh [--password-admin]' >&2; exit 1 ;;
esac
# Quote SQL literals with standard quote doubling; do not put passwords in argv.
password_sql="${DB_PASSWORD//\'/\'\'}"
# MySQL database GRANT patterns treat underscores as wildcards, even inside backticks.
grant_db="${DB_NAME//_/\\_}"
if ! "${admin[@]}" --batch <<SQL 2>/dev/null
SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES';
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'localhost' IDENTIFIED BY '$password_sql';
ALTER USER '$DB_USERNAME'@'localhost' IDENTIFIED BY '$password_sql';
GRANT ALL PRIVILEGES ON \`$grant_db\`.* TO '$DB_USERNAME'@'localhost';
SQL
then
    echo 'Database setup failed. Check MySQL is running and administrator socket access works (sudo mysql), or use --password-admin. SQL diagnostics are hidden to protect credentials.' >&2
    exit 1
fi
printf 'Database %s is ready for %s@localhost. Start with ./run-backend.sh\n' "$DB_NAME" "$DB_USERNAME"
