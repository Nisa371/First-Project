#!/usr/bin/env bash
set -euo pipefail
backend_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$backend_dir/load-env.sh"
load_backend_env "$backend_dir/.env"
cd "$backend_dir"
exec ./mvnw spring-boot:run "$@"
