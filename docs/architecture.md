# V0.1 架构

## 设计与运行

设计流程：需求 → 规则／可选模型 → PageSchema 校验 → 结构预览／代码预览 → 人工确认。

运行流程：注册 Schema → 动态导航 → CmsPage → typed API client → Spring Security → ContentService → JPA / Flyway → 审计。

浏览器中的 agent-core 无密钥、无文件系统访问；CLI 只输出到新的审查目录。后端模型接口是规划适配器，不是任意工具执行器。

## 包边界

- contracts：纯 TypeScript 契约和校验，不依赖 Vue。
- agent-core：只依赖 contracts，确定性规划与生成。
- api-client：请求、错误、Session 和 CSRF。
- ui：Schema 渲染列表、分页、表单、确认操作。
- admin：应用布局、登录、工作台、路由与审计。
- backend：按 schema / security / module / content / agent / audit / common 组织。

Java PageSchema 无 Spring 依赖，可以 javac 单独验证。TypeScript、Java 和 spec/page.schema.json 同步维护。跨字段重复、至少一个列表字段等语义约束仍由运行时代码执行。

## 数据模型

cms_module(code, revision, schema_json) 是不可变模块定义。revision 用于 JPA 新实体识别和并发控制，不是对外 Schema 版本。

content_entry(id, module_code, data_json, search_text, version, created_at, updated_at) 保存经过规范校验的内容。写入时更新搜索文本；LIKE 通配符转义；查询和 ID 操作都限定 module_code。更新／删除同时验证客户端版本并依赖数据库乐观锁。

audit_event 在同一业务事务内记录操作者、动作、目标，不存正文。读取接口返回最近 100 条，长期保留／归档需继续实现。

通用 JSON 表减少首版成本，但不适合复杂关系、字段索引、大规模全文检索或每模块独立数据库约束。下一版应增加独立表生成器／存储适配器，而不是无限扩充同一张 JSON 表。

## 生成物

Vue SFC 复用 CmsPage；Java ApplicationRunner 调用 ModuleService.installIfAbsent。生成物是本平台的模块插件，不是独立部署项目。

工作台创建模块只保存元数据，由动态页面立即使用。CLI 输出用于代码评审，不会自动应用、执行或 push 到其他仓库。

## 后续优先级

先补目标仓库适配器（可配置目录、路由和 API 约定、AST 检查），再做 Schema diff／迁移审批、独立业务 Entity / DTO / Service / SQL 生成器。随后增加 Agent 任务持久化、生成后编译反馈、Git PR 工作流、SSO 与数据权限。

关系字段、媒体上传、富文本编辑、审批流、站点发布、向量检索、自动执行仓库指令不属于本版已实现能力。
