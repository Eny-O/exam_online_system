#!/usr/bin/env bash
# 本地一键起依赖（中间件 + MySQL）
#
# 该脚本只负责「依赖层」：
#   - 常驻中间件：Nacos / Sentinel / RocketMQ（始终启动）
#   - 数据存储：MySQL（仅 --profile full 时启动，会按 sql/ 里的
#     01-schema.sql、02-init-data.sql 自动建库建表并灌入初始数据）
# 然后等待依赖就绪，并打印「业务服务 + 前端」的启动顺序。
# 业务服务（auth-user/core/ai/gateway）与前端在宿主机运行，不在 compose 内。
#
# 用法（在仓库根目录执行）：
#   bash scripts/up.sh
#
# 说明：
#   - 若使用老版本 Docker（docker-compose 连字符），把下面 DOCKER_COMPOSE 改成 docker-compose
#   - Redis 目前未纳入 compose（app 仍连 localhost:6379），需本地另起或后续补充

set -euo pipefail

DOCKER_COMPOSE="docker compose"

# 切到仓库根目录（本文件位于 <repo>/scripts/ 下）
cd "$(dirname "$0")/.."

echo "==> 启动中间件（含 profile=full 的 MySQL）"
$DOCKER_COMPOSE --profile full up -d

# 等待 docker health 变为 healthy（带超时，超时仅告警不中断）
wait_health() {
  local name="$1"; local max="${2:-150}"; local i=0
  echo "==> 等待 $name 就绪（最多 ${max}s）"
  while [ "$i" -lt "$max" ]; do
    local st
    st=$(docker inspect -f '{{.State.Health.Status}}' "$name" 2>/dev/null || true)
    if [ "$st" = "healthy" ]; then
      echo "    $name 已就绪"
      return 0
    fi
    sleep 2; i=$((i + 2))
  done
  echo "    [警告] $name 在 ${max}s 内未变为 healthy，继续（请稍后手动确认 docker ps / 日志）"
}

# 等待宿主端口可达（用于没有 HTTP health 的服务，如 RocketMQ）
wait_port() {
  local host="$1"; local port="$2"; local max="${3:-150}"; local label="$4"; local i=0
  echo "==> 等待 $label ($host:$port) 就绪（最多 ${max}s）"
  while [ "$i" -lt "$max" ]; do
    if (exec 3<>"/dev/tcp/$host/$port") 2>/dev/null; then
      echo "    $label 已就绪"
      return 0
    fi
    sleep 2; i=$((i + 2))
  done
  echo "    [警告] $label 在 ${max}s 内不可达，继续（请稍后手动确认）"
}

wait_health nacos 150
wait_health exam-mysql 150
wait_port 127.0.0.1 9876 150 "RocketMQ NameServer"
wait_port 127.0.0.1 10911 150 "RocketMQ Broker"
wait_port 127.0.0.1 16379 150 "Redis"

echo
echo "============================================================"
echo " 依赖已就绪。请在【宿主机】按以下顺序启动业务服务与前端："
echo "============================================================"
echo " 1) auth-user   端口 8082   cd exam_cloud/exam_auth_user && mvn spring-boot:run"
echo " 2) exam-core   端口 8081   cd exam_cloud/exam-core    && mvn spring-boot:run"
echo " 3) exam-ai     端口 8083   cd exam_cloud/exam_ai      && mvn spring-boot:run"
echo " 4) gateway     端口 8080   cd exam_cloud/exam_gateway && mvn spring-boot:run"
echo " 5) 前端        cd exam-vue && npm install && npm run dev"
echo "------------------------------------------------------------"
echo " 注意：MySQL、Redis 与 Nacos 均已容器化；"
echo "       app 当前仍按 localhost:3306 / localhost:6379 连接，"
echo "       若改用容器映射端口，需把 datasource/redis 改为"
echo "       127.0.0.1:13306 / 127.0.0.1:16379（或加 Spring profile）。"
echo "============================================================"
