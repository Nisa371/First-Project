#!/usr/bin/env bash
# Shared by the two launchers; parse literal values without executing shell code.
load_backend_env() {
    local file="$1" line key value line_number=0
    if [[ -f "$file" ]]; then
        while IFS= read -r line || [[ -n "$line" ]]; do
            line_number=$((line_number + 1))
            line="${line%$'\r'}"
            [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
            if [[ ! "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
                printf 'Invalid .env format at line %s; use KEY=value.\n' "$line_number" >&2
                return 1
            fi
            key="${BASH_REMATCH[1]}" value="${BASH_REMATCH[2]}"
            if [[ "$value" == \"*\" || "$value" == \'*\' ]]; then
                value="${value:1:${#value}-2}"
            fi
            if [[ ! -v "$key" ]]; then
                export "$key=$value"
            fi
        done < "$file"
    fi
    export DB_HOST="${DB_HOST:-localhost}" DB_PORT="${DB_PORT:-3306}"
    export DB_NAME="${DB_NAME:-verified_career_marketplace}"
    export DB_USERNAME="${DB_USERNAME:-marketplace}"
    # Public local/demo default, matching application-dev.yml. Never use for production.
    if [[ -z "${DB_PASSWORD+x}" && "${SPRING_PROFILES_ACTIVE:-dev}" == dev ]]; then
        export DB_PASSWORD=marketplace123
    fi
    if [[ -z "${DB_PASSWORD:-}" ]]; then
        printf 'Set a nonempty DB_PASSWORD in backend/.env or the environment.\n' >&2
        return 1
    fi
    if [[ "${DB_USERNAME,,}" == root ]]; then
        printf 'Use a dedicated DB_USERNAME (for example marketplace), not root.\n' >&2
        return 1
    fi
}
