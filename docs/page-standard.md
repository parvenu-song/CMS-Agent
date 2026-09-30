# 页面规范 V1

```json
{"version":1,"code":"article","title":"文章管理","fields":[
  {"name":"title","label":"标题","type":"text","required":true,"list":true},
  {"name":"status","label":"状态","type":"select","required":true,"list":true,"options":["draft","published","archived"]}
]}
```

模块标识 2–32 位小写 ASCII 字母／数字，字母开头。字段名 1–32 位 camelCase ASCII；禁止系统保留字段和原型相关键。字段 1–30 个，名字唯一，至少一个列表字段。页面标题、标签和选项最多 80 字符。

| 类型 | Vue 控件 | 数据约束 |
| --- | --- | --- |
| text | Input | 字符串，最多 200 字符 |
| textarea | Textarea | 字符串，最多 20,000 字符，不渲染 HTML |
| number | InputNumber | 有限数字，绝对值不超过 10^12 |
| boolean | Switch | true / false，false 不视为空 |
| select | Select | 1–20 个唯一字符串选项，仅接受声明值 |

required 和 list 必须明确写为布尔值。可选空值归一化为 null。未知字段、类型、属性被拒绝。status 是普通 select，不是审批状态机或公开发布入口。

## API 约定

同源 Session。所有写请求带 GET /api/auth/csrf 返回的安全令牌，header 名称使用 headerName。登录后重新取得令牌。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | /api/auth/csrf | CSRF 令牌 |
| POST | /api/auth/login | URL-encoded username / password |
| GET | /api/auth/me | 当前用户和角色 |
| POST | /api/auth/logout | 注销，204 |
| GET / POST | /api/modules | 列出／创建模块 |
| GET / POST | /api/content/{code} | 分页列表／新增 |
| GET / PUT / DELETE | /api/content/{code}/{id} | 读取／更新／删除 |
| GET | /api/audit | 管理员查看最近 100 条变更 |
| GET | /api/agent/status | 可选模型配置状态 |
| POST | /api/agent/plan | 管理员显式模型规划 |

page 从 0 开始；size 1–100，默认 10；q 最多 100 字符。固定按 updatedAt 倒序、id 正序，不接受任意 SQL 排序输入。

列表响应 {items,total,page,size}，记录 {id,version,data,createdAt,updatedAt}。新增 {data:{...}}，更新 {data:{...},version:当前版本}，删除 ?version=当前版本。401 未登录，403 权限／CSRF，404 不存在，409 版本／唯一性冲突，400 参数错误。错误响应包含 message，不含异常栈或模型原始错误。

## 页面约定

列表包含标题、总数、分页、加载／空／错误状态、刷新与新增。编辑使用表单弹窗，客户端必填检查不替代服务端验证。删除需确认且限 ADMIN。模块切换时清理旧搜索和表单，迟到的列表响应不得覆盖新模块。
