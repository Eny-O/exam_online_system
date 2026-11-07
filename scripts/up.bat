@echo off
REM 本地一键起依赖（中间件 + MySQL）
REM
REM 只负责「依赖层」：Nacos / Sentinel / RocketMQ（常驻）+ MySQL（profile:full）。
REM 业务服务（auth-user/core/ai/gateway）与前端在宿主机运行，见末尾启动顺序。
REM
REM 用法（仓库根目录）： scripts\up.bat
REM 若使用老版本 Docker，把下面 DOCKER_COMPOSE 改为 docker-compose

set "DOCKER_COMPOSE=docker compose"
cd /d "%~dp0.."

echo ==^> 启动中间件（含 profile=full 的 MySQL）
%DOCKER_COMPOSE% --profile full up -d

echo ==^> 等待依赖就绪...
call :wait_health nacos 150
call :wait_health exam-mysql 150
call :wait_port 127.0.0.1 9876 150 "RocketMQ NameServer"
call :wait_port 127.0.0.1 10911 150 "RocketMQ Broker"
call :wait_port 127.0.0.1 16379 150 "Redis"

echo.
echo ============================================================
echo  依赖已就绪。请在【宿主机】按以下顺序启动业务服务与前端：
echo ============================================================
echo  1^) auth-user   端口 8082   cd exam_cloud\exam_auth_user ^&^& mvn spring-boot:run
echo  2^) exam-core   端口 8081   cd exam_cloud\exam-core    ^&^& mvn spring-boot:run
echo  3^) exam-ai     端口 8083   cd exam_cloud\exam_ai      ^&^& mvn spring-boot:run
echo  4^) gateway     端口 8080   cd exam_cloud\exam_gateway ^&^& mvn spring-boot:run
echo  5^) 前端        cd exam-vue ^&^& npm install ^&^& npm run dev
echo ------------------------------------------------------------
echo  注意：MySQL、Redis 与 Nacos 均已容器化；
echo         app 当前仍按 localhost:3306 / localhost:6379 连接，
echo         若改用容器映射端口，需把 datasource/redis 改为
echo         127.0.0.1:13306 / 127.0.0.1:16379（或加 Spring profile）。
echo ============================================================
goto :eof

:wait_health
set "name=%~1"
set "max=%~2"
set /a i=0
echo ==^> 等待 %name% 就绪（最多 %max%s）
:health_loop
set "st="
for /f "usebackq delims=" %%s in (`docker inspect -f "{{.State.Health.Status}}" %name% 2^>nul`) do set "st=%%s"
if "%st%"=="healthy" ( echo     %name% 已就绪 & goto :eof )
if %i% geq %max% ( echo     [警告] %name% 在 %max%s 内未变为 healthy，继续 & goto :eof )
timeout /t 2 >nul
set /a i+=2
goto :health_loop

:wait_port
set "host=%~1"
set "port=%~2"
set "max=%~3"
set "label=%~4"
set /a i=0
echo ==^> 等待 %label% ^(%host%:%port%^) 就绪（最多 %max%s）
:port_loop
powershell -NoProfile -Command "Test-NetConnection -ComputerName %host% -Port %port% -InformationLevel Quiet" | findstr /i "True" >nul && ( echo     %label% 已就绪 & goto :eof )
if %i% geq %max% ( echo     [警告] %label% 在 %max%s 内不可达，继续 & goto :eof )
timeout /t 2 >nul
set /a i+=2
goto :port_loop
