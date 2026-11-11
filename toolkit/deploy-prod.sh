#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
SERVER="${BETTA_DEPLOY_SERVER:-root@106.13.8.170}"
SSH_OPTIONS=(-o StrictHostKeyChecking=yes)

for command in git java mvn node npm tar ssh scp; do
    command -v "$command" >/dev/null || { echo "缺少命令: $command" >&2; exit 1; }
done
cd "$PROJECT_DIR"
[ "$(git symbolic-ref --quiet --short HEAD || true)" = prod ] || {
    echo '请先切换到本地 prod 分支，已停止部署。' >&2; exit 1;
}
SOURCE_COMMIT=$(git rev-parse --verify 'HEAD^{commit}')
echo "本次构建：本地 prod $SOURCE_COMMIT"
WORKTREE_STATUS=$(git status --porcelain)
if [ -n "$WORKTREE_STATUS" ]; then
    echo '注意：本次构建使用当前工作区，未提交修改也可能进入制品。' >&2
fi
# Vue CLI 还会读取被 Git 忽略的本地环境文件，仅提示路径，不输出配置内容。
for local_env in .env.local .env.production.local; do
    if [ -f "betta-ui/$local_env" ]; then
        echo "注意：betta-ui/$local_env 会影响前端生产构建。" >&2
    fi
done

LOCAL_STAGE=$(mktemp -d "${TMPDIR:-/tmp}/betta-deploy.XXXXXXXX")
cleanup() {
    rm -rf "$LOCAL_STAGE"
}
trap cleanup EXIT

echo '在本机打包后端...'
mvn clean package -DskipTests
echo '在本机打包前端...'
(
    cd betta-ui
    # 检查 Mermaid 实际解析到的 uuid，避免已有的嵌套版本导致 Vue CLI 4 构建失败。
    frontend_dependencies_ready() {
        [ -x node_modules/.bin/vue-cli-service ] && npm ls --depth=0 --include=dev >/dev/null 2>&1 && node -e '
            try {
                const mermaid = require.resolve("mermaid/package.json");
                const uuid = require.resolve("uuid/package.json", { paths: [mermaid] });
                if (require(uuid).version !== "9.0.1") process.exit(1);
            } catch (error) {
                process.exit(1);
            }
        '
    }
    if ! frontend_dependencies_ready; then
        echo '前端依赖缺失或不兼容，安装兼容版本...'
        npm install --no-audit --no-fund --no-save --no-package-lock --include=dev uuid@9.0.1
        frontend_dependencies_ready || { echo '前端依赖仍不兼容，已停止部署。' >&2; exit 1; }
    fi
    npm run build:prod
)
[ -f betta-admin/target/betta-admin.jar ] && [ -f betta-ui/dist/index.html ] || {
    echo '前后端制品不完整' >&2; exit 1;
}

REMOTE_STAGE=''
cp betta-admin/target/betta-admin.jar "$LOCAL_STAGE/backend.jar"
tar -czf "$LOCAL_STAGE/frontend.tar.gz" -C betta-ui/dist .
REMOTE_STAGE=$(ssh "${SSH_OPTIONS[@]}" "$SERVER" 'mktemp -d /tmp/betta-deploy.XXXXXXXX')
echo '上传部署文件...'
scp "${SSH_OPTIONS[@]}" "$LOCAL_STAGE/backend.jar" "$LOCAL_STAGE/frontend.tar.gz" "$SERVER:$REMOTE_STAGE/"

ssh "${SSH_OPTIONS[@]}" "$SERVER" bash -s -- "$REMOTE_STAGE" <<'REMOTE'
set -euo pipefail
stage=$1
app=/data/app
jar=$app/Betta1_0.jar
env_file=$app/betta.env
log=$app/Betta1_0.log
web=/data/web/Betta1_0
app_backup=''
web_backup=''
new_pid=''
old_stopped=0
backend_replaced=0
frontend_replaced=0
had_jar=0
had_web=0
interrupted=0
in_rollback=0

fail() { echo "错误：$*" >&2; exit 1; }
for cmd in java nginx systemctl lsof tar flock; do command -v "$cmd" >/dev/null || fail "缺少命令 $cmd"; done
exec 9>/tmp/betta-deploy.lock
flock -n 9 || fail '已有部署正在执行'
java_version=$(java -version 2>&1 | head -n 1 | sed -E 's/.*version "([0-9]+).*/\1/')
[[ "$java_version" =~ ^[0-9]+$ ]] && [ "$java_version" -ge 17 ] || fail '需要 Java 17 或更高版本'
[ -r "$env_file" ] || fail "环境文件不可读取：$env_file"
[ -f "$stage/backend.jar" ] && [ -s "$stage/frontend.tar.gz" ] || fail '上传制品不完整'
mkdir -p "$app/backup" /data/web/backup "$stage/frontend"
touch "$log" || fail '日志不可写'
tar -xzf "$stage/frontend.tar.gz" -C "$stage/frontend"
[ -f "$stage/frontend/index.html" ] || fail '前端制品缺少 index.html'
nginx -t
systemctl is-active --quiet nginx || fail 'Nginx 未运行'

# 复用原脚本的环境变量契约；只在 Java 启动时导出。
unset BETTA_FRONT_BASE_URL ENG_PRONUNCIATION_ENABLED ENG_PRONUNCIATION_TENCENT_APP_ID ENG_PRONUNCIATION_TENCENT_SECRET_ID ENG_PRONUNCIATION_TENCENT_SECRET_KEY
source "$env_file"
for name in BETTA_FRONT_BASE_URL ENG_PRONUNCIATION_ENABLED ENG_PRONUNCIATION_TENCENT_APP_ID ENG_PRONUNCIATION_TENCENT_SECRET_ID ENG_PRONUNCIATION_TENCENT_SECRET_KEY; do
    [ -n "${!name:-}" ] || fail "环境变量未配置：$name"
    export -n "$name" 2>/dev/null || true
done

start_backend() {
    trap 'interrupted=1' HUP INT TERM
    (
        export BETTA_FRONT_BASE_URL ENG_PRONUNCIATION_ENABLED
        export ENG_PRONUNCIATION_TENCENT_APPID="$ENG_PRONUNCIATION_TENCENT_APP_ID"
        export ENG_PRONUNCIATION_TENCENT_SECRETID="$ENG_PRONUNCIATION_TENCENT_SECRET_ID"
        export ENG_PRONUNCIATION_TENCENT_SECRETKEY="$ENG_PRONUNCIATION_TENCENT_SECRET_KEY"
        cd "$app"
        exec nohup java -Xms256M -Xmx512M -jar "$jar" </dev/null >> "$log" 2>&1 9>&-
    ) &
    new_pid=$!
    if [ "$in_rollback" -eq 0 ]; then trap rollback HUP INT TERM; else trap '' HUP INT TERM; fi
    [ "$interrupted" -eq 0 ]
}
wait_started() {
    local pid=$1 i
    for ((i=0; i<30; i++)); do
        kill -0 "$pid" 2>/dev/null || return 1
        if lsof -nP -a -p "$pid" -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then return 0; fi
        sleep 2
    done
    return 1
}
stop_pid() {
    local pid=$1 i
    kill -TERM "$pid" 2>/dev/null || true
    for ((i=0; i<15; i++)); do
        kill -0 "$pid" 2>/dev/null || return 0
        sleep 1
    done
    kill -KILL "$pid" 2>/dev/null || true
    for ((i=0; i<5; i++)); do
        kill -0 "$pid" 2>/dev/null || return 0
        sleep 1
    done
    return 1
}
rollback() {
    local failed=0
    trap - ERR HUP INT TERM
    in_rollback=1
    echo '部署失败，尝试恢复原有制品...' >&2
    if [ -n "$new_pid" ] && ! stop_pid "$new_pid"; then
        echo '新进程未停止，跳过自动恢复以免覆盖运行中的制品' >&2
        exit 1
    fi
    if [ "$frontend_replaced" -eq 1 ] && { [ "$had_web" -eq 0 ] || [ -e "$web_backup" ]; }; then
        if rm -rf "$web"; then
            if [ -n "$web_backup" ]; then mv "$web_backup" "$web" || failed=1; fi
        else
            echo '前端目录清理失败，保留备份供人工恢复' >&2
            failed=1
        fi
    fi
    if [ "$backend_replaced" -eq 1 ] && { [ "$had_jar" -eq 0 ] || [ -f "$app_backup" ]; }; then
        rm -f "$jar" || failed=1
        if [ -n "$app_backup" ]; then mv "$app_backup" "$jar" || failed=1; fi
    fi
    if [ "$old_stopped" -eq 1 ] && [ -f "$jar" ] && ! kill -0 "$listener" 2>/dev/null; then
        interrupted=0
        if ! start_backend || ! wait_started "$new_pid"; then
            echo '旧版后端启动失败' >&2
            failed=1
        fi
    elif [ "$old_stopped" -eq 1 ] && kill -0 "$listener" 2>/dev/null; then
        echo '旧进程仍存在，请人工检查其状态' >&2
    fi
    [ "$failed" -eq 0 ] || echo '自动恢复未完成，请人工检查' >&2
    exit 1
}

# 仅允许停止与目标 JAR 匹配的监听进程，不影响其他 8080 服务。
listener=$(lsof -tiTCP:8080 -sTCP:LISTEN || true)
if [ -n "$listener" ]; then
    [ "$(printf '%s\n' "$listener" | wc -l | tr -d ' ')" = 1 ] || fail '8080 有多个监听进程'
    tr '\0' '\n' < "/proc/$listener/cmdline" | grep -Fx -- "$jar" >/dev/null || fail '8080 被其他进程占用'
fi
stamp=$(date +%Y%m%d-%H%M%S)
suffix="${stamp}-$$"
trap rollback ERR HUP INT TERM
if [ -n "$listener" ]; then
    old_stopped=1
    stop_pid "$listener" || { echo '旧后端无法停止，请人工检查' >&2; false; }
fi
if [ -e "$jar" ]; then
    had_jar=1
    app_backup="$app/backup/Betta1_0.jar_$suffix"
fi
backend_replaced=1
if [ "$had_jar" -eq 1 ]; then
    mv "$jar" "$app_backup"
fi
cp "$stage/backend.jar" "$jar"
if [ -e "$web" ]; then
    had_web=1
    web_backup="/data/web/backup/Betta1_0_$suffix"
fi
frontend_replaced=1
if [ "$had_web" -eq 1 ]; then
    mv "$web" "$web_backup"
fi
mv "$stage/frontend" "$web"
start_backend
if ! wait_started "$new_pid"; then
    tail -n 80 "$log" >&2 || true
    false
fi
systemctl reload nginx
trap - ERR HUP INT TERM
echo '部署成功：后端已监听 8080，Nginx 已重载。'
REMOTE
ssh "${SSH_OPTIONS[@]}" "$SERVER" bash -s -- "$REMOTE_STAGE" <<'CLEANUP' || echo "临时目录清理失败：$REMOTE_STAGE" >&2
case "$1" in /tmp/betta-deploy.*) rm -rf -- "$1" ;; esac
CLEANUP
