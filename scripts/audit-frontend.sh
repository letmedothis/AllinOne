#!/bin/sh
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
audit_failed=0
audit_scope=${1:-production}

if [ "$audit_scope" != "production" ] && [ "$audit_scope" != "--all" ]; then
    echo "Usage: $0 [--all]" >&2
    exit 2
fi

audit_project() {
    project_name=$1
    project_dir=$2

    echo "Auditing production dependencies: $project_name"
    cd "$project_dir"
    if ! npm audit --omit=dev --audit-level=high; then
        audit_failed=1
    fi

    if [ "$audit_scope" = "--all" ]; then
        echo "Auditing all dependencies: $project_name"
        if ! npm audit --audit-level=high; then
            audit_failed=1
        fi
    fi
}

audit_project "Luckysheet" "$PROJECT_ROOT/allinone-luckysheet"
audit_project "application frontend" "$PROJECT_ROOT/allinone-typescript"

exit "$audit_failed"
