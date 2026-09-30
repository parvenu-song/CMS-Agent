# CMS-Agent

**面向内容管理后台的 Schema 驱动 Agent 与全栈开发底座。**

Vue 3 + TypeScript + Ant Design Vue + pnpm Monorepo；后端 Java 21 + Spring Boot 4.1.0。第一版实现“需求 → 规范 → 代码预览 → 创建模块 → 持久化 CRUD”，不是只有聊天窗口或静态后台模板。

> V0.1 是工程初版，不是已完成生产验证的通用自主编码 Agent。规则模式无需模型密钥；可选模型服务需要单独配置。React Ant Design 项目不会被自动转换成 Vue。

## 已实现

| 模块 | 能力 |
| --- | --- |
| Agent 工作台 | 关键词规划、可编辑 Schema、结构预览、6 文件代码预览与 JSON 包下载 |
| 生成 CLI | 从 Schema 或工作台 bundle 输出 Vue 页面、路由、Java 模块注册器和接入说明；不覆盖已有目录 |
| 仓库识别 | 检查固定位置 package.json，识别 Vue / React / Ant Design Vue；不执行仓库脚本 |
| 内容管理 | 动态导航、服务端分页搜索、新增、编辑、删除、校验、加载／空／错误状态 |
| 后端 | 模块与内容持久化、Flyway 迁移、乐观锁、统一错误响应 |
| 安全基础 | Session、HttpOnly Cookie、CSRF、ADMIN / EDITOR 权限、事务内变更审计 |
| 工程 | Node 测试、Java 契约检查、Spring 集成测试、HTTP smoke、Docker Compose、CI 配置 |

**存储选择：** `cms_module` 保存规范，`content_entry` 保存 JSON 内容，`audit_event` 保存变更元数据。生成的 Java 注册器复用平台内容 API；**不是为每个模块生成独立 Entity、SQL 表或独立项目**。

## 本地启动

需要 Node.js 22.12+、pnpm 10.18.3、JDK 21、Maven 3.9+。进入本版本分支后：

```bash
corepack enable
pnpm install --no-frozen-lockfile
```

终端一启动后端。开发环境使用 H2 文件数据库，无需安装 PostgreSQL：

```bash
cd backend
mvn spring-boot:run
```

终端二在仓库根目录启动前端：

```bash
pnpm dev
```

访问 `http://localhost:5173`。后端监听 `127.0.0.1:8080`，Vite 代理 `/api`，无需开放跨域。

仅限 dev profile 的演示账户：

| 用户 | 密码 | 权限 |
| --- | --- | --- |
| admin | dev-admin-change-me | 模块创建、内容读写删除、审计、模型规划 |
| editor | dev-editor-change-me | 内容读取、新增与编辑；不能删除、创建模块或查看审计 |

开发环境自动安装“文章管理”Schema，不写入虚构内容。数据库在后端进程工作目录的 `.data/` 下，重启保留数据。

首次联网安装会生成 `pnpm-lock.yaml`。当前提交未包含经联网解析的锁文件；请审查并提交首次生成的锁文件，然后将 CI / Docker 安装命令切换为 `--frozen-lockfile`。未锁定的传递依赖仍可能变化。

## 创建第一个模块

登录工作台，填写标识 `news`、名称“资讯管理”，描述“标题、正文、作者、分类、状态”。点击“生成页面规范”，检查 Schema 和代码，再点击“创建模块”。新模块进入侧栏，内容管理页面连接真实 API，可新增、搜索、编辑、删除。

规则模式只识别内置关键词，不是大模型。复杂需求可手动编辑 Schema，或显式调用已配置模型。结构预览不会写入数据库，只有点击“创建模块”才提交。

## CLI 和已有仓库

```bash
# 只检查 package.json，不安装依赖、不执行脚本
pnpm agent inspect /path/to/your-admin-repository

# 输出目录必须不存在
pnpm agent generate examples/article.schema.json --out .generated/article

# 也支持工作台下载的 bundle；忽略包内路径，按 Schema 重新生成
pnpm agent generate /path/to/news.bundle.json --out .generated/news
```

每个模块输出 6 个文件及 SHA-256 清单：

```text
schemas/<code>.json
apps/admin/src/generated/<code>/schema.ts
apps/admin/src/generated/<code>/Page.vue
apps/admin/src/generated/<code>/route.ts
backend/src/main/java/com/cmsagent/generated/<Name>Module.java
docs/generated/<code>.md
manifest.json
```

先审查 diff，再将 apps/ 和 backend/ 下文件合入 **CMS-Agent 结构的项目**。后端扫描注册器，现有导航自动发现模块。自定义生成页面的 route 需要手动加入 Router。相同 Schema 幂等跳过，已存在且不同的 Schema 会导致启动失败，不隐式覆盖数据语义。

其他 Vue 后台需要接入共享 contracts / api-client / ui 包、API 和路由约定。仓库检查只覆盖根目录、apps/admin、apps/web 的 package.json，不是完整 AST／路由扫描或自动改造工具，不会自动向目标仓库 push。

## 可选模型规划

支持返回 `choices[0].message.content` 的 chat-completions 兼容服务。配置完整 HTTPS endpoint、模型名称和服务端密钥后重启，工作台会显示“发送需求至模型规划”。PowerShell 示例：

```powershell
$env:CMS_AGENT_URL="https://your-model-service.example/v1/chat/completions"
$env:CMS_AGENT_MODEL="your-model-name"
$env:CMS_AGENT_KEY="your-server-side-api-key"
cd backend
mvn spring-boot:run
```

默认不发起模型请求。显式点击会向配置的服务发送需求文本，密钥不下发浏览器。模型输出必须通过后端 Schema 校验并保持模块标识／标题，否则返回错误，不执行命令、不静默降级、不自动创建模块。未使用真实提供商密钥联调。

## Docker Compose

```bash
cp .env.example .env
# PowerShell: Copy-Item .env.example .env
# 替换三个不同的随机密码；登录密码至少 16 字符、最多 72 UTF-8 字节
# 仅 localhost HTTP 演示保留 CMS_COOKIE_SECURE=false
docker compose up --build
```

访问 `http://localhost:8088`。Compose 使用 PostgreSQL + Spring Boot + Nginx，数据库保存在命名卷。仅 Web 绑定主机回环地址，数据库与 API 不暴露主机端口。prod 不自动安装演示模块。

`.env` 自动加载仅适用于 Compose。直接运行 Maven 时使用环境变量。生产必须配置 HTTPS、`CMS_COOKIE_SECURE=true`、独立密码和数据库备份。当前只有两个环境配置账户，不包括用户管理、SSO、多租户或细粒度数据权限；不要直接暴露到公网。

## 验证

```bash
pnpm test
pnpm typecheck
pnpm build
mvn -B -ntp -f backend/pom.xml verify
# 后端已运行时进行真实 Cookie / CSRF / CRUD 检查
node scripts/smoke.mjs
```

无第三方 Java 依赖的契约检查：

```bash
javac -d .generated/schema-check backend/src/main/java/com/cmsagent/schema/PageSchema.java scripts/SchemaCheck.java
java -cp .generated/schema-check SchemaCheck
```

CI 配置包含前端类型检查和构建、Spring 集成测试、生成 Java 注册器后的编译，以及真实 HTTP smoke。以实际 Actions 结果为准，不应将“已配置 CI”视为“已通过 CI”。编写时环境无法联网下载 npm / Maven 依赖；已本地运行 16 项 Node 核心测试、12 项 Java 契约检查和核心 TypeScript 类型检查，完整前后端构建、浏览器联调、PostgreSQL 和模型提供商联调仍需联网验证。

HTTP smoke 会创建测试模块并删除其中的内容，但不删除模块定义；请只在开发／测试数据库执行。

## 目录

```text
apps/admin/              Vue 管理端
packages/contracts/      类型、Schema 和数据校验
packages/agent-core/     规则规划、代码模板、安全 CLI
packages/api-client/     Session / CSRF / HTTP 客户端
packages/ui/             Ant Design Vue 通用内容页面
backend/                 Spring Boot 内容服务
examples/                可用示例规范
spec/                    JSON Schema
scripts/                 契约与 HTTP 检查
deploy/                  Nginx
docs/                    架构与页面规范
```

详见 [架构](docs/architecture.md)、[页面规范](docs/page-standard.md)、[安全说明](SECURITY.md)。保留原仓库 Apache-2.0 许可证。
