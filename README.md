# 在线考试系统（Exam Online System）

> 基于 **Spring Boot 3 + Vue3** 的在线考试平台，覆盖「题库管理 → 组卷 → 考试 → 交卷 → 批阅 → 成绩统计」闭环；后端按 Spring Cloud Alibaba 拆分为认证、业务、AI、网关等多服务，AI 模块提供阅卷辅助能力。

[![Java](https://img.shields.io/badge/Java-17-orange)](https://img.shields.io/badge/Java-17-orange)
[![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
[![SpringCloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue)](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue)
[![Vue](https://img.shields.io/badge/Vue-3.x-42b883)](https://img.shields.io/badge/Vue-3.x-42b883)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1)](https://img.shields.io/badge/MySQL-8.0-4479a1)
[![License](https://img.shields.io/badge/license-MIT-blue)](https://img.shields.io/badge/license-MIT-blue)

---

## 项目预览

| | | |
| :---: | :---: | :---: |
| <img src="assets/preview-login.png" width="260"/> | <img src="assets/preview-questionbank.png" width="260"/> | <img src="assets/preview-exam.png" width="260"/> |
| 登录页 | 题库管理 | 考试答题 |
| <img src="assets/preview-grading.png" width="260"/> | <img src="assets/preview-grade.png" width="260"/> | <img src="assets/preview-result.png" width="260"/> |
| 主观题批阅 | 成绩详情 | 考试结果 |

> 覆盖三条角色线与完整业务闭环：登录 → 题库管理 → 考试答题 → 主观题批阅 → 成绩详情 / 考试结果。截图位于 `assets/`，命名约定 `preview-{login,questionbank,exam,grading,grade,result}.png`。

## 一、技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2.5 · Spring Cloud Alibaba 2023.0.1.0 · Spring Cloud Gateway · Spring Security + JWT |
| 数据 | MyBatis-Plus 3.5.16 · MySQL 8.0.31 · Redis 7 · 乐观锁并发控制 |
| 中间件 | Nacos 2.3.2（注册/配置）· Sentinel 1.8.6（限流/熔断）· RocketMQ 5.3.2（异步批阅） |
| AI 能力 | LangChain4j（exam_ai：阅卷辅助）|
| 前端 | Vue 3 · Vite · Element Plus · Axios |
| 文档 | Knife4j（OpenAPI 3） |

---

## 二、仓库结构

单仓库存放全部服务，但每个服务**独立 Maven 工程、独立 jar、独立部署**。

```
exam_online_system/                # 仓库根
├── docker-compose.yml             # 中间件编排
├── assets/                        # 项目预览截图
├── sql/                           # 建库建表 + 种子数据
│   ├── schema.sql                 # 双库 + 14 张表
│   └── init-data.sql              # 角色/权限/班级/默认账号 种子数据
├── scripts/                       # 一键起中间件
│   ├── up.sh                      # Linux / macOS
│   └── up.bat                     # Windows
├── exam_cloud/                    # 后端微服务
│   ├── exam_common/               # 共享库（不独立运行）
│   ├── exam_api/                  # 公共 API / Feign 契约（不独立运行）
│   ├── exam_auth_user/            # 认证与权限服务  :8082  库 exam_auth_db
│   ├── exam-core/                 # 业务核心服务    :8081  库 exam_core_db
│   ├── exam_ai/                   # AI 辅助服务     :8083
│   └── exam_gateway/              # API 网关        :8080
└── exam-vue/                      # 前端（Vue3 + Vite + ElementPlus）
```

---

## 三、中间件端口

全部中间件通过 `docker-compose.yml` 编排。注册/配置类组件做了宿主机端口避让（见下文「Windows 端口说明」）。

| 组件 | 容器端口 | 宿主机端口 | 说明 |
|------|---------|-----------|------|
| Nacos | 8848 / 9848 / 9849 | **18848** / 19848 / 19849 | Nacos 2.x gRPC 端口 = HTTP + 1000 |
| Sentinel | 8858 / 8719 | **18858** / 18719 | 控制台 + 客户端心跳 |
| RocketMQ NameServer | 9876 | 9876 | 路由发现 |
| RocketMQ Broker | 10909 / 10911 / 10912 | 10909 / 10911 / 10912 | 标准端口，未做映射 |
| MySQL | 3306 | **13306** | 仅 `full` profile |
| Redis | 6379 | **16379** | 仅 `full` profile，无密码 |

> MySQL / Redis 用高位端口（`13306` / `16379`）是为了不和你本机已安装的 3306 / 6379 冲突；Nacos / Sentinel 同理避让 Windows 保留段。

---

## 四、快速开始

### 4.1 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- Docker + Docker Compose

### 4.2 启动中间件

```bash
# Windows
scripts\up.bat

# Linux / macOS
bash scripts/up.sh
```

脚本等价于 `docker compose --profile full up -d`，并在启动后等待依赖就绪（Nacos / MySQL / Redis 健康检查 + RocketMQ 端口探活），最后打印后端启动顺序。

- 默认 `docker compose up -d`（不带 `--profile full`）只起 **Nacos / Sentinel / RocketMQ**。
- 加 `--profile full` 会**额外拉起 MySQL 与 Redis**，并把 `sql/schema.sql`、`sql/init-data.sql` 自动挂载进 MySQL 容器初始化目录执行（文件名前缀 `01-` / `02-` 控制顺序）。

### 4.3 初始化数据

使用 `--profile full` 启动 MySQL 后，**无需手动执行任何 SQL**：

- `schema.sql`：创建双库（`exam_auth_db` / `exam_core_db`）与全部 14 张表；
- `init-data.sql`：写入角色、菜单/按钮权限、一个班级，以及三名默认账号（见 4.6）；
- 数据持久化在 `mysql_data` 卷中，容器重启不会重复初始化。

> 若你本机已自行安装 MySQL / Redis 并希望复用，把各服务的 `application.yml` 中数据源地址改回 `localhost:3306` / `localhost:6379` 即可。

### 4.4 配置后端服务

每个服务都提供了 `application.yml.example`（可入库的样例，**不含敏感信息**）。复制为 `application.yml` 后填值：

```bash
for s in exam_auth_user exam-core exam_ai exam_gateway; do
  cp exam_cloud/$s/src/main/resources/application.yml.example \
     exam_cloud/$s/src/main/resources/application.yml
done
```

需要确认/填写的占位：

| 配置项 | 示例值 | 说明 |
|--------|--------|------|
| MySQL 地址 / 密码 | `127.0.0.1:13306` / `123456` | 与 compose 中 `MYSQL_ROOT_PASSWORD` 一致 |
| Redis 地址 | `127.0.0.1:16379` | 无密码 |
| `exam.internal-api-key` | 自定义字符串 | `exam_auth_user` 与 `exam-core` 两侧**必须相同** |
| `AI_API_KEY`（仅 exam_ai） | 你的模型 Key | 通过环境变量或 `.example` 中的占位填入 |

> `application.yml` 本身被 `.gitignore` 排除，不会入库；请基于 `.example` 在本地生成。

### 4.5 启动服务与前端

**后端（按依赖顺序）：**

```bash
cd exam_cloud/exam_auth_user && mvn spring-boot:run   # :8082 认证与权限
cd exam_cloud/exam-core      && mvn spring-boot:run   # :8081 业务核心
cd exam_cloud/exam_ai        && mvn spring-boot:run   # :8083 AI 辅助
cd exam_cloud/exam_gateway   && mvn spring-boot:run   # :8080 API 网关
```

**前端：**

```bash
cd exam-vue
npm install
npm run dev        # 启动后访问终端输出的本地地址
```

### 4.6 默认账号

所有默认账号密码均为 `123456`（已用 BCrypt 加密写入种子数据）：

| 用户名 | 姓名 | 角色 | 权限范围 |
|--------|------|------|---------|
| `admin` | 系统管理员 | ROLE_ADMIN | 全量（人员 / 权限 / 班级 / 系统管理） |
| `teacher` | 张老师 | ROLE_TEACHER | 题库 / 试卷 / 考试 / 阅卷 / 班级 |
| `student` | 李同学 | ROLE_STUDENT | 考试中心 / 成绩查询 / 个人中心 |

### 4.7 常用访问地址

| 地址 | 说明 |
|------|------|
| `http://localhost:8080` | API 网关入口（前端通过该地址访问后端） |
| `http://localhost:8082/doc.html` 等 | 各服务 Knife4j 接口文档（端口对应 4.5） |
| `http://127.0.0.1:18848/nacos` | Nacos 控制台（nacos / nacos） |
| `http://127.0.0.1:18858` | Sentinel 控制台 |

---

## 五、Windows 端口说明（排障）

若中间件在 Windows 上偶发「端口被占用 / 连不上」，常见根因是 **TCP 动态端口范围被设得过宽**（如 `1024-65535`），导致 Hyper-V / `winnat` 的排除段吞掉项目端口。永久修复（管理员 PowerShell / CMD）：

```powershell
netsh int ipv4 set dynamicport tcp start=49152 num=16383
netsh int ipv6 set dynamicport tcp start=49152 num=16383
# 执行后重启系统
```

重启后动态范围收敛到 `49152-65535`，项目所用的 `18848` / `18858` / `18719` 等高位端口即永久安全。若仍异常，先执行 `netsh interface ipv4 show excludedportrange protocol=tcp` 查看当前排除段再判断。

---

## 六、说明

本项目为个人学习与作品展示用途，架构决策围绕「渐进式演进」展开，部署与排障细节见上方各章节。

## License

MIT
