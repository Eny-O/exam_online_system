#!/usr/bin/env bash
# 本地开发一键起依赖层（中间件 + MySQL + Redis）
#
# 显式指定服务名，只启动 compose 中的依赖层：
#   nacos / sentinel / rmqnamesrv / rmqbroker / mysql / redis
# MySQL 首次启动会按 sql/ 里的 01-schema.sql、02-init-data.sql
# 自动建库建表并灌入初始数据。
# 然后等待依赖就绪，并打印「业务服务 + 前端」的启动顺序。
# 业务服务（auth-user/core/ai/gateway）与前端在宿主机运行，不在此脚本内启动。
#
# 用法（在仓库根目录执行）：
#   bash scripts/up.sh
#
# 说明：
#   - 若使用老版本 Docker（docker-compose 连字符），把下面 DOCKER_COMPOSE 改成 docker-compose
#   - 一键部署全套（含 4 个后端与前端）请直接在仓库根执行 docker compose up -d

set -euo pipefail

DOCKER_COMPOSE="docker compose"

# 切到仓库根目录（本文件位于 <repo>/scripts/ 下）
cd "$(dirname "$0")/.."

echo "==> 启动依赖层（中间件 + MySQL + Redis）"
$DOCKER_COMPOSE up -d nacos sentinel rmqnamesrv rmqbroker mysql redis

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
wait_port 127.0.0.1 18858 150 "Sentinel Dashboard"
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
echo "       本地开发的 application.yml 需把数据源 / Redis 指向"
echo "       127.0.0.1:13306 / 127.0.0.1:16379，"
echo "       Nacos 指向 127.0.0.1:18848（见 README 5.4）。"
echo "============================================================"
