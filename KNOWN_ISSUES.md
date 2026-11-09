# 已知未解决问题（Backlog）

> 本文件记录已定位但暂未处理的代码问题，供日后集中处理。
> 最后更新：2026-11-09

---

## 一、ExamTask.vue 未使用函数（ESLint / TS 报错）

**现象**：`src/views/teacher/ExamTask/ExamTask.vue` 中两个函数声明后从未在模板绑定，触发
- TS：`已声明 "xxx"，但从未读取其值。（ts-plugin 6133）`
- ESLint：`'xxx' is assigned a value but never used.（no-unused-vars）`

| 函数 | 行号 | 函数体 |
|------|------|--------|
| `handleViewScore` | 363–365 | `router.push(\`/teacher/classExamScore?examId=${row.id}\`)` |
| `handlePublishScore` | 434–436 | `router.push(\`/teacher/classExamScore?examId=${row.id}\`)` |

**根因**：模板"操作"列按状态渲染按钮，但「批阅完成(5)」状态只渲染了"删除考试"，没有"查看成绩 / 公布成绩"入口；这两个函数疑似当时想给状态 5 接入口、但没接上模板留下的死代码。两者目标路由完全相同（重复定义）。

**待决策（二选一）**：
- **A. 直接删除两个函数**：彻底消掉 lint 报错，最省事。代价：批阅完成后无"查看成绩"入口（若该功能不需要，最干净）。
- **B. 接进模板**：在「批阅完成(5)」状态加"查看成绩"按钮绑定 `handleViewScore`；`handlePublishScore` 建议删除（与查看成绩同路由，且"公布成绩"通常还应调发布状态接口，现状仅跳转实现不对）。
- **推荐 B**：成绩查看是考试闭环真实缺失的一环。

**注意**：若选 B 给"公布成绩"做真实发布，`handlePublishScore` 不应只做跳转，需补调用发布成绩状态的后端接口（当前无对应实现，需后端配合）。

---

## 二、ExamTask.vue `size` 属性笔误（小 bug）

**位置**：`src/views/teacher/ExamTask/ExamTask.vue` 第 102 行（未发布状态的"发布"按钮）。
**现状**：`size="smal l"`（多了空格），应为 `size="small"`。
**影响**：Element Plus 不识别该值，按钮尺寸回退默认，非致命但应修。

---

## 三、待提交改动（非问题，提醒用）

- 前端 59 个文件 CRLF → LF 归一化、新增仓库根 `.gitattributes`（`* text=auto eol=lf`）/`.editorconfig`（`end_of_line = lf`）、`exam-vue/.prettierrc.json` 回归默认 `{}`，**均未提交**，建议尽快 commit（message 见对话记录）。
- 根目录 `README.md` 改写与 `permission_init.sql` 删除已在之前提交（不在待提交列表）。

---

## 四、已知待改进（历史遗留，非本次引入）

- **RocketMQ 消费端**：业务层确定性失败（如"答案不存在"）目前也会重试 16 次后进 DLQ，属已知待改进点。应区分"脏报文直接丢弃"与"业务瞬时异常才重试"两种策略。

---

## 五、Feign 跨服务调用无 fallback（用户服务挂了整请求 500）

**现象**：`exam-core` 通过 `exam_api` 的 Feign 客户端调用 `auth-user` 拿用户名/班级名，但**调用处没有任何熔断/降级/异常兜底**。当 `auth-user` 宕机时，相关接口直接返回 500，而非降级打开。

**受影响调用点**（`exam-core/.../exam/service/impl/ExamManagementServiceImpl.java`）：
- L120 `userFeignClient.listUserBriefs(creatorIds)`（考试列表创建人名）
- L185 `userFeignClient.listTeacherClassIds(userId)`（教师权限范围校验 `assertTeacherScope`）
- L196 `userFeignClient.listUserBriefs(...)`（学生→班级映射）
- L427 `userFeignClient.listUserBriefs(List.of(creatorId))`（考试详情创建人名）
- L456 `userFeignClient.listUserBriefs(studentIds)` / L465 `classFeignClient.listClassBriefs(classIds)`（批阅学生列表）

**根因**：
- 5 个 Feign 客户端（`exam_api/feign/` 下 `UserFeignClient`/`ClassFeignClient`/`StudentAnswerFeignClient`/`QuestionFeignClient`/`ExamClassFeignClient`）**全部未声明 `fallbackFactory`**。
- 无 `feign.sentinel.enabled=true`，无 `@SentinelResource` 包 Feign，无 `ErrorDecoder`/`Retryer`/resilience4j 替代方案。
- 调用处的 `if (creators != null && !creators.isEmpty())` 之类判断**只处理"调用成功但结果为空"**，不捕获"auth-user 不可达"抛出的 `feign.RetryableException`。

**实际后果**：异常冒泡到 `exam-core/.../handler/GlobalExceptionHandler.java` 的 `@ExceptionHandler(Exception.class)`（L94-98），返回 `ResponseResult.error(ErrorCodeEnum.INTERNAL_ERROR)`。即：用户服务一挂，考试列表/详情/批阅列表/教师权限校验**全部 500**，不是"列表能开、名字空缺"。

**重要区分**：简历第 4 条写的"空值保护"（`if (sysClass != null)` 等）**只覆盖"数据缺失"，覆盖不了"服务宕机"**。面试被问"用户服务挂了考试列表还能开吗"——真实答案是"不能，会 500"。

**已知史实（避免误会）**：`exam_ai/pom.xml:53-54` 有一段注释说"Feign 客户端声明了 fallbackFactory，必须开 feign.sentinel.enabled 否则启动报错"，但当前 5 个客户端均**无** fallbackFactory——说明曾经写过、后来删了、注释未同步清理（属过时注释 bug，见下章）。

**简历风险**：第 4 条"通过 Feign 批量拉取摘要数据并做空值保护"易被解读为有容错；实际缺服务宕机兜底。建议面试主动澄清"空值保护只覆盖数据缺失，服务宕机需补 fallback"。

**待决策（二选一）**：
- **A. 补 fallbackFactory**：给 `UserFeignClient`/`ClassFeignClient` 加 `fallbackFactory`，auth-user 不可用时返回空列表，列表页降级打开（名字空缺而非 500）；并开启 `feign.sentinel.enabled=true`。可坐实"服务治理"。
- **B. 暂不改**：面试层面以"有意识取舍（版本兼容风险 + 个人项目可用性要求）"应对。

---

## 六、exam_ai/pom.xml 过时注释（小清理）

**位置**：`exam_cloud/exam_ai/pom.xml` 第 53–54 行注释。
**现状**：注释叙述"Feign 客户端声明了 fallbackFactory，必须带 Sentinel 且 feign.sentinel.enabled=true 才能实例化，否则启动报错"，但当前 `exam_api` 下 5 个 Feign 客户端**均未声明 fallbackFactory**，该注释与代码不符。
**影响**：误导维护者/阅读源码者，且面试看源码会被带偏。
**处理**：删除或改写为真实情况（当前未用 Feign 熔断）。

---

## 七、容器化一键部署（2026-11-09 实施）

为让他人快速跑起来，已落地全容器化方案，默认路径：`docker compose up -d` 拉起全套（中间件 + MySQL/Redis + 4 后端 + 前端，应用镜像来自阿里云 ACR，无需本机构建）。

**已落地内容：**
- 各后端服务新增 `application-docker.yml` + `bootstrap-docker.yml`（激活 `SPRING_PROFILES_ACTIVE=docker`），把 `localhost` 系地址改写为容器服务名（`mysql`/`redis`/`rmqnamesrv`/`nacos`/`sentinel`）；`exam_ai` 的 `api-key` 改为读 `${AI_API_KEY}` 环境变量。
- `docker/` 下 4 个多阶段 Dockerfile（maven 构建 → JRE 运行）；`exam-vue/` 下 Dockerfile（node 构建 → nginx 托管）+ `nginx.conf`（静态托管 + `/api` 代理到 `gateway:8080`）+ `.dockerignore`。
- 根 `docker-compose.yml` 整合全套，`mysql`/`redis` 去掉 `profiles:["full"]` 限制；应用服务用 `depends_on` + 健康检查保证启动顺序。
- `.github/workflows/build-push.yml`：GitHub Actions 构建并推送 5 个应用镜像到**阿里云 ACR**（需在 GitHub 仓库 Secrets 配置 `ALIYUN_USER`/`ALIYUN_PASSWORD`/`ACR_NAMESPACE`）。

**已知待优化 / 注意点（非阻塞）：**
- **AI 密钥**：不提供 `AI_API_KEY` 时 `exam-ai` 仍可启动，但调用大模型批阅会失败，属预期。需在项目根 `.env` 提供密钥。
- **构建效率**：4 个后端镜像各自 `mvn -pl X -am package`，重复编译 reactor（exam_common/exam_api）。可改为单次 reactor 构建 + 多镜像 COPY 优化，当前为可读性优先。
- **`scripts/up.sh`/`.bat`**：仍带 `--profile full` 参数，现已无害（无服务再声明该 profile，等同 `docker compose up`），可后续简化为纯 `docker compose up -d`。
- **Maven 依赖缓存**：Docker 内 `mvn package` 每次构建都重新拉取依赖（构建层不跨 `docker build` 复用），首次/改代码后构建较慢，CI 机可挂 `~/.m2` 缓存卷加速。
- **配置来源**：刻意不走 Nacos 配置中心存值，docker profile 文件即配置源，保证克隆即跑、可复现。
- **`.env` 已加入 `.gitignore`**，密钥不入库；已新增 `.env.example` 模板（含 `IMAGE_PREFIX` 与 `AI_API_KEY`），别人 fork 后改名即可。

