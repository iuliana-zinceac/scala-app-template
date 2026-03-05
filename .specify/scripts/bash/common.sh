#!/usr/bin/env bash
# Common functions and variables for all scripts

# Get repository root, with fallback for non-git repositories
get_repo_root() {
    if git rev-parse --show-toplevel >/dev/null 2>&1; then
        git rev-parse --show-toplevel
    else
        # Fall back to script location for non-git repos
        local script_dir="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
        (cd "$script_dir/../../.." && pwd)
    fi
}

# Get current branch, with fallback for non-git repositories
get_current_branch() {
    # First check if SPECIFY_FEATURE environment variable is set
    if [[ -n "${SPECIFY_FEATURE:-}" ]]; then
        echo "$SPECIFY_FEATURE"
        return
    fi

    # Then check git if available
    if git rev-parse --abbrev-ref HEAD >/dev/null 2>&1; then
        git rev-parse --abbrev-ref HEAD
        return
    fi

    # For non-git repos, try to find the latest feature directory
    local repo_root=$(get_repo_root)
    local specs_dir="$repo_root/specs"

    if [[ -d "$specs_dir" ]]; then
        local latest_feature=""
        local latest_mtime=0

        for dir in "$specs_dir"/*/; do
            if [[ -d "$dir" ]]; then
                local dirname=$(basename "$dir")
                local mtime
                mtime=$(stat -c %Y "$dir" 2>/dev/null || stat -f %m "$dir" 2>/dev/null || echo 0)
                if [[ "$mtime" -gt "$latest_mtime" ]]; then
                    latest_mtime=$mtime
                    latest_feature=$dirname
                fi
            fi
        done

        if [[ -n "$latest_feature" ]]; then
            echo "$latest_feature"
            return
        fi
    fi

    echo "main"  # Final fallback
}

# Check if we have git available
has_git() {
    git rev-parse --show-toplevel >/dev/null 2>&1
}

check_feature_branch() {
    local branch="$1"
    local has_git_repo="$2"

    # For non-git repos, we can't enforce branch naming but still provide output
    if [[ "$has_git_repo" != "true" ]]; then
        echo "[specify] Warning: Git repository not detected; skipped branch validation" >&2
        return 0
    fi

    # Accept git-flow type prefixes: feat/, fix/, chore/, docs/, test/, refactor/, style/, perf/, ci/, build/, hotfix/
    # Accept task-number prefixes: TASK-1/, JIRA-123/, ABC-456/, etc.
    if [[ "$branch" =~ ^(feat|fix|chore|docs|test|refactor|style|perf|ci|build|hotfix)/.+ ]] || \
       [[ "$branch" =~ ^[A-Z]+-[0-9]+/.+ ]]; then
        return 0
    fi

    echo "ERROR: Branch '$branch' does not follow naming convention." >&2
    echo "Expected: feat/name, fix/name, chore/name, ... or TASK-1/description" >&2
    return 1
}

get_feature_dir() { echo "$1/specs/$2"; }

# Sanitize a branch name for use as a filesystem directory name.
# Replaces '/' with '-' and lowercases the result.
# e.g., feat/add-ai-support  → feat-add-ai-support
#       TASK-1/balala         → task-1-balala
sanitize_branch_for_dir() {
    echo "$1" | tr '/' '-' | tr '[:upper:]' '[:lower:]'
}

# Find the spec directory for the current branch.
# For task-number branches (e.g., TASK-1/name) a prefix lookup on the task ID
# (lowercased, e.g., task-1) is used so multiple branches can share one spec.
# For type branches (e.g., feat/name) an exact match on the sanitized name is used.
find_feature_dir() {
    local repo_root="$1"
    local branch_name="$2"
    local specs_dir="$repo_root/specs"
    local sanitized
    sanitized=$(sanitize_branch_for_dir "$branch_name")

    # Task-number branch: do prefix lookup on the task ID (e.g., task-1-*)
    if [[ "$branch_name" =~ ^([A-Z]+-[0-9]+)/ ]]; then
        local task_prefix
        task_prefix=$(echo "${BASH_REMATCH[1]}" | tr '[:upper:]' '[:lower:]')
        local matches=()
        if [[ -d "$specs_dir" ]]; then
            for dir in "$specs_dir"/"$task_prefix"-*; do
                [[ -d "$dir" ]] && matches+=("$(basename "$dir")")
            done
        fi
        if [[ ${#matches[@]} -eq 1 ]]; then
            echo "$specs_dir/${matches[0]}"
            return
        elif [[ ${#matches[@]} -gt 1 ]]; then
            echo "ERROR: Multiple spec directories found with prefix '$task_prefix': ${matches[*]}" >&2
            echo "Please ensure only one spec directory exists per task prefix." >&2
        fi
    fi

    # Default: use sanitized branch name as directory name
    echo "$specs_dir/$sanitized"
}

get_feature_paths() {
    local repo_root=$(get_repo_root)
    local current_branch=$(get_current_branch)
    local has_git_repo="false"

    if has_git; then
        has_git_repo="true"
    fi

    local feature_dir
    feature_dir=$(find_feature_dir "$repo_root" "$current_branch")

    cat <<EOF
REPO_ROOT='$repo_root'
CURRENT_BRANCH='$current_branch'
HAS_GIT='$has_git_repo'
FEATURE_DIR='$feature_dir'
FEATURE_SPEC='$feature_dir/spec.md'
IMPL_PLAN='$feature_dir/plan.md'
TASKS='$feature_dir/tasks.md'
RESEARCH='$feature_dir/research.md'
DATA_MODEL='$feature_dir/data-model.md'
QUICKSTART='$feature_dir/quickstart.md'
CONTRACTS_DIR='$feature_dir/contracts'
EOF
}

check_file() { [[ -f "$1" ]] && echo "  ✓ $2" || echo "  ✗ $2"; }
check_dir() { [[ -d "$1" && -n $(ls -A "$1" 2>/dev/null) ]] && echo "  ✓ $2" || echo "  ✗ $2"; }

