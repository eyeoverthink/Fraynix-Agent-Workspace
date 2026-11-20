
#!/usr/bin/env bash
set -euo pipefail

root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
prepare=0
offline=0
for option in "$@"; do
    case "$option" in
        --prepare) prepare=1 ;;
        --offline) offline=1 ;;
        --help|-h)
            printf '%s\n' 'Usage: ./Launch-Agent-Workspace.sh [--prepare] [--offline]' \
                'Use --prepare after downloading or updating. Run without it to open the workspace.'
            exit 0 ;;
        *) printf 'Unknown option: %s\n' "$option" >&2; exit 2 ;;
    esac
done

case "$(uname -s)" in
    Darwin)
        if [[ -z "${JAVA_HOME:-}" ]]; then
            JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null) || {
                printf '%s\n' 'Install a JDK 21 for your Mac. See README.md.' >&2
                exit 1
            }
            export JAVA_HOME
        fi ;;
    Linux) ;;
    *) printf '%s\n' 'On Windows run Launch-Agent-Workspace.bat.' >&2; exit 1 ;;
esac

if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! command -v node >/dev/null || ! node -e 'process.exit(Number(process.versions.node.split(".")[0]) < 22 ? 1 : 0)'; then
    printf '%s\n' 'Install Node.js 24 LTS and reopen Terminal. See README.md.' >&2
    exit 1
fi
javac_version=$(javac -version 2>&1) || javac_version=
if [[ ! "$javac_version" =~ ^javac[[:space:]]21([.]|$) ]]; then
    printf 'Select JDK 21 with JAVA_HOME. Found: %s\n' "${javac_version:-no javac}" >&2
    exit 1
fi
if [[ -n "${JAVA_HOME:-}" && ! -x "$JAVA_HOME/bin/java" ]]; then
    printf '%s\n' 'JAVA_HOME does not contain an executable Java 21 runtime.' >&2
    exit 1
fi
if (( offline )); then export FRAYNIX_WORKSPACE_OFFLINE=1; fi
desktop="$root/workspace-desktop"

if (( prepare )); then
    # An open interface or its detached Java owner may still hold this installation.
    commands=$(ps -ax -o command=)
    case "$commands" in
        *"$root/workspace-engine/build/install/workspace-engine/lib"*|*"$root/build/install/Fraynix-Studio/lib"*)
            printf '%s\n' 'This checkout is in use. Choose Stop Java & close app before preparing an update.' >&2
            exit 1 ;;
    esac
    gradle_args=(--console=plain :prepareGameRuntime115 :workspace-engine:installDist)
    if (( offline )); then gradle_args+=(--offline); fi
    (cd -- "$root" && ./gradlew "${gradle_args[@]}")
    if ! node "$root/scripts/Verify-Desktop-Cache.cjs" "$desktop" >/dev/null 2>&1; then
        if (( offline )); then
            printf '%s\n' 'Desktop dependencies are missing. Run --prepare online once.' >&2
            exit 1
        fi
        (cd -- "$desktop" && npm ci --include=dev --no-audit --no-fund && node node_modules/electron/install.js)
    fi
    node "$root/scripts/Verify-Desktop-Cache.cjs" "$desktop"
    printf '%s\n' 'Workspace prepared. Run ./Launch-Agent-Workspace.sh to open it.'
    exit
fi

if [[ ! -d "$root/workspace-engine/build/install/workspace-engine/lib" ||
      ! -d "$root/build/install/Fraynix-Studio/lib" ]]; then
    printf '%s\n' 'Run ./Launch-Agent-Workspace.sh --prepare first.' >&2
    exit 1
fi
node "$root/scripts/Verify-Desktop-Cache.cjs" "$desktop"
cd -- "$desktop"
exec node node_modules/electron/cli.js .
