#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR=$(cd "$(dirname "$0")/../.." && pwd)
TEST_ROOT=$(mktemp -d "${TMPDIR:-/tmp}/betta-deploy-test.XXXXXXXX")
trap 'rm -rf "$TEST_ROOT"' EXIT

fail() { echo "测试失败：$*" >&2; exit 1; }

LOCAL_REPO="$TEST_ROOT/local"
TEST_BIN="$TEST_ROOT/bin"
mkdir -p "$TEST_BIN" "$TEST_ROOT/tmp" "$TEST_ROOT/capture"
export TMPDIR="$TEST_ROOT/tmp"
export TEST_TRACE="$TEST_ROOT/trace"
export TEST_CAPTURE="$TEST_ROOT/capture"
export BETTA_DEPLOY_SERVER='test@example.invalid'
unset TEST_FAIL_PHASE

# 在临时仓库模拟本地 prod 落后、远端不可用及前后端未提交修改。
OLDER=$(git -C "$PROJECT_DIR" rev-parse --verify 'prod^')
git clone --quiet --no-hardlinks --branch prod -- "$PROJECT_DIR" "$LOCAL_REPO"
git -C "$LOCAL_REPO" checkout --quiet -B prod "$OLDER"
git -C "$LOCAL_REPO" remote set-url origin "$TEST_ROOT/missing-remote"
mkdir -p "$LOCAL_REPO/toolkit" "$LOCAL_REPO/betta-ui/node_modules/.bin"
cp "$PROJECT_DIR/toolkit/deploy-prod.sh" "$LOCAL_REPO/toolkit/deploy-prod.sh"
printf '\n// BETTA_LOCAL_ONLY_MARKER\n' >> "$LOCAL_REPO/betta-admin/src/main/java/com/betta/BettaApplication.java"
printf '\n// BETTA_LOCAL_ONLY_MARKER\n' >> "$LOCAL_REPO/betta-ui/src/main.js"
printf '#!/usr/bin/env bash\n' > "$LOCAL_REPO/betta-ui/node_modules/.bin/vue-cli-service"
chmod +x "$LOCAL_REPO/betta-ui/node_modules/.bin/vue-cli-service"
touch "$LOCAL_REPO/betta-ui/node_modules/.uuid9"

# 构建和远程命令全部替换为测试替身，不连接生产服务器。
cat > "$TEST_BIN/mvn" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
printf 'mvn:%s\n' "$*" >> "$TEST_TRACE"
[ "$*" = 'clean package -DskipTests' ] || exit 20
[ "${TEST_FAIL_PHASE:-}" != backend ] || exit 21
grep -q BETTA_LOCAL_ONLY_MARKER betta-admin/src/main/java/com/betta/BettaApplication.java
mkdir -p betta-admin/target
{ git rev-parse HEAD; grep BETTA_LOCAL_ONLY_MARKER betta-admin/src/main/java/com/betta/BettaApplication.java; } > betta-admin/target/betta-admin.jar
MOCK
cat > "$TEST_BIN/node" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
[ "${1:-}" = -e ] || exit 26
[ -f node_modules/.uuid9 ]
MOCK
cat > "$TEST_BIN/npm" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
case "${1:-}" in
    install)
        printf 'npm_install:%s\n' "$*" >> "$TEST_TRACE"
        [ "$*" = 'install --no-audit --no-fund --no-save --no-package-lock --include=dev uuid@9.0.1' ] || exit 22
        [ "${TEST_FAIL_PHASE:-}" != install ] || exit 23
        mkdir -p node_modules/.bin
        if [ "${TEST_FAIL_PHASE:-}" != missing_cli ]; then
            printf '#!/usr/bin/env bash\n' > node_modules/.bin/vue-cli-service
            chmod +x node_modules/.bin/vue-cli-service
        fi
        if [ "${TEST_FAIL_PHASE:-}" != incompatible ]; then touch node_modules/.uuid9; fi
        if [ "${TEST_FAIL_PHASE:-}" != invalid_direct ]; then rm -f node_modules/.invalid_direct; fi
        ;;
    ls)
        printf 'npm_check\n' >> "$TEST_TRACE"
        [ "$*" = 'ls --depth=0 --include=dev' ] || exit 28
        [ ! -f node_modules/.invalid_direct ]
        ;;
    run)
        [ "${2:-}" = build:prod ] || exit 24
        printf 'npm_build\n' >> "$TEST_TRACE"
        [ "${TEST_FAIL_PHASE:-}" != frontend ] || exit 25
        grep -q BETTA_LOCAL_ONLY_MARKER src/main.js
        mkdir -p dist
        { git rev-parse HEAD; grep BETTA_LOCAL_ONLY_MARKER src/main.js; } > dist/index.html
        ;;
    *) exit 27 ;;
esac
MOCK
cat > "$TEST_BIN/ssh" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
printf 'ssh\n' >> "$TEST_TRACE"
case "$*" in
    *'mktemp -d /tmp/betta-deploy.'*) printf '/tmp/betta-deploy.mock\n' ;;
    *) cat >/dev/null ;;
esac
MOCK
cat > "$TEST_BIN/scp" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
printf 'scp\n' >> "$TEST_TRACE"
for arg in "$@"; do
    case "$arg" in
        */backend.jar) cp "$arg" "$TEST_CAPTURE/backend.jar" ;;
        */frontend.tar.gz) cp "$arg" "$TEST_CAPTURE/frontend.tar.gz" ;;
    esac
done
[ -f "$TEST_CAPTURE/backend.jar" ] && [ -f "$TEST_CAPTURE/frontend.tar.gz" ]
MOCK
chmod +x "$TEST_BIN/mvn" "$TEST_BIN/node" "$TEST_BIN/npm" "$TEST_BIN/ssh" "$TEST_BIN/scp"
export PATH="$TEST_BIN:$PATH"

assert_clean() {
    local path
    for path in "$TMPDIR"/betta-deploy.*; do
        [ ! -e "$path" ] || fail "临时目录未清理：$path"
    done
}

assert_no_upload() {
    if [ -f "$TEST_TRACE" ] && grep -Eq '^(ssh|scp)$' "$TEST_TRACE"; then
        fail '失败时不应连接服务器或上传文件'
    fi
    assert_clean
}

run_failure() {
    local phase=$1
    rm -f "$TEST_TRACE"
    if TEST_FAIL_PHASE="$phase" bash "$LOCAL_REPO/toolkit/deploy-prod.sh" > "$TEST_ROOT/output" 2>&1; then
        fail "$phase 失败场景意外成功"
    fi
    assert_no_upload
}

# 即使远端不可用，也必须用当前本地 prod 和未提交代码构建前后端。
bash "$LOCAL_REPO/toolkit/deploy-prod.sh" > "$TEST_ROOT/output" 2>&1
[ "$(head -n 1 "$TEST_CAPTURE/backend.jar")" = "$OLDER" ] || fail '后端未使用本地 prod 提交'
grep -q BETTA_LOCAL_ONLY_MARKER "$TEST_CAPTURE/backend.jar" || fail '后端未包含本地修改'
tar -xOzf "$TEST_CAPTURE/frontend.tar.gz" ./index.html > "$TEST_ROOT/frontend-index"
[ "$(head -n 1 "$TEST_ROOT/frontend-index")" = "$OLDER" ] || fail '前后端提交不一致'
grep -q BETTA_LOCAL_ONLY_MARKER "$TEST_ROOT/frontend-index" || fail '前端未包含本地修改'
grep -q "$OLDER" "$TEST_ROOT/output" || fail '未输出本次构建提交号'
grep -q '未提交修改也可能进入制品' "$TEST_ROOT/output" || fail '未提示工作区修改'
grep -qx scp "$TEST_TRACE" || fail '成功时没有进入上传步骤'
if grep -q '^npm_install:' "$TEST_TRACE"; then fail '已有兼容依赖时不应安装'; fi
grep -q BETTA_LOCAL_ONLY_MARKER "$LOCAL_REPO/betta-admin/src/main/java/com/betta/BettaApplication.java" || fail '本地后端改动被覆盖'
grep -q BETTA_LOCAL_ONLY_MARKER "$LOCAL_REPO/betta-ui/src/main.js" || fail '本地前端改动被覆盖'
assert_clean
echo '通过：本地 prod、远端不可用、工作区修改和兼容依赖复用'

# 不兼容依赖和缺少 Vue CLI 时安装一次，之后再次构建应复用。
rm -f "$TEST_TRACE" "$LOCAL_REPO/betta-ui/node_modules/.uuid9"
bash "$LOCAL_REPO/toolkit/deploy-prod.sh" > "$TEST_ROOT/output" 2>&1
grep -q '^npm_install:' "$TEST_TRACE" || fail '依赖不兼容时没有安装'
assert_clean
rm -f "$TEST_TRACE"
bash "$LOCAL_REPO/toolkit/deploy-prod.sh" > "$TEST_ROOT/output" 2>&1
if grep -q '^npm_install:' "$TEST_TRACE"; then fail '已修复的依赖未被复用'; fi
touch "$LOCAL_REPO/betta-ui/node_modules/.invalid_direct"
rm -f "$TEST_TRACE"
bash "$LOCAL_REPO/toolkit/deploy-prod.sh" > "$TEST_ROOT/output" 2>&1
grep -q '^npm_install:' "$TEST_TRACE" || fail '直接依赖不兼容时没有安装'
rm -f "$TEST_TRACE" "$LOCAL_REPO/betta-ui/node_modules/.bin/vue-cli-service"
bash "$LOCAL_REPO/toolkit/deploy-prod.sh" > "$TEST_ROOT/output" 2>&1
grep -q '^npm_install:' "$TEST_TRACE" || fail '缺少 Vue CLI 时没有安装'
assert_clean
echo '通过：缺少或不兼容依赖时安装，以及后续复用'

# Git 忽略的 Vue 本地环境文件也应提示，但不能输出其中的配置内容。
printf 'VUE_APP_RESOURCE=BETTA_ENV_SECRET_MARKER\n' > "$LOCAL_REPO/betta-ui/.env.local"
printf 'VUE_APP_BASE_API=BETTA_ENV_SECRET_MARKER\n' > "$LOCAL_REPO/betta-ui/.env.production.local"
git -C "$LOCAL_REPO" check-ignore --quiet betta-ui/.env.local || fail '.env.local 应被 Git 忽略'
git -C "$LOCAL_REPO" check-ignore --quiet betta-ui/.env.production.local || fail '.env.production.local 应被 Git 忽略'
rm -f "$TEST_TRACE"
bash "$LOCAL_REPO/toolkit/deploy-prod.sh" > "$TEST_ROOT/output" 2>&1
grep -q 'betta-ui/.env.local 会影响' "$TEST_ROOT/output" || fail '未提示 .env.local'
grep -q 'betta-ui/.env.production.local 会影响' "$TEST_ROOT/output" || fail '未提示 .env.production.local'
if grep -q BETTA_ENV_SECRET_MARKER "$TEST_ROOT/output"; then fail '不应输出本地环境文件内容'; fi
rm "$LOCAL_REPO/betta-ui/.env.local" "$LOCAL_REPO/betta-ui/.env.production.local"
assert_clean
echo '通过：提示被忽略的本地环境文件且不泄露内容'

# 非 prod、构建失败和安装后仍不兼容均不得上传。
git -C "$LOCAL_REPO" checkout --quiet -b other-branch
run_failure branch
if [ -f "$TEST_TRACE" ] && grep -q '^mvn:' "$TEST_TRACE"; then fail '非 prod 分支不应开始构建'; fi
git -C "$LOCAL_REPO" checkout --quiet prod
run_failure backend
run_failure frontend
rm -f "$LOCAL_REPO/betta-ui/node_modules/.uuid9"
run_failure install
run_failure incompatible
touch "$LOCAL_REPO/betta-ui/node_modules/.invalid_direct"
run_failure invalid_direct
rm -f "$LOCAL_REPO/betta-ui/node_modules/.invalid_direct"
rm -f "$LOCAL_REPO/betta-ui/node_modules/.bin/vue-cli-service"
run_failure missing_cli
echo '通过：非 prod、构建或依赖失败时不上传'
