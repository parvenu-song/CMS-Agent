import { parseSchema } from '../../contracts/src/index.ts';
import type { Field, PageSchema } from '../../contracts/src/index.ts';
export interface PlanInput { code: string; title: string; requirement: string }
export interface Artifact { path: string; content: string }
export interface Plan { engine: 'rules'; schema: PageSchema; steps: string[]; warnings: string[] }
const field = (name: string,label: string,type: Field['type']='text',required=false,list=true): Field => ({name,label,type,required,list});
/** Deterministic, intentionally limited keyword planner; this is not a hidden LLM. */
export function plan(input: PlanInput): Plan {
  if (!input.requirement.trim() || input.requirement.length > 4000) throw new Error('需求长度必须为 1–4000');
  const q = input.requirement.toLowerCase();
  const fields: Field[] = [field('title','标题','text',true)];
  const catalog: [RegExp, Field][] = [
    [/内容|正文|content|body/,field('content','正文','textarea',true,false)],
    [/摘要|简介|summary|description/,field('summary','摘要','textarea',false,false)],
    [/作者|author/,field('author','作者')], [/分类|category/,field('category','分类')],
    [/价格|price/,field('price','价格','number',true)], [/库存|stock/,field('stock','库存','number')],
    [/排序|权重|sort/,field('sortOrder','排序','number')], [/启用|enabled/,field('enabled','启用','boolean')],
  ];
  for (const [pattern,f] of catalog) if (pattern.test(q)) fields.push(f);
  fields.push({...field('status','状态','select',true),options:['draft','published','archived']});
  return {engine:'rules',schema:parseSchema({version:1,code:input.code,title:input.title,fields}),
    steps:['提取受支持的业务字段','应用列表／表单与校验规范','验证 Schema 安全约束','预览后显式创建模块'],
    warnings:['规则模式只识别内置关键词；复杂关系、审批流和权限需求需要人工建模。','发布状态仅是字段，不代表已实现审批或公开发布。']};
}
/** Paths are fixed and code is validated; arbitrary user template execution is prohibited. */
export function generate(raw: unknown): Artifact[] {
  const schema = parseSchema(raw), code = schema.code;
  const name = code[0].toUpperCase()+code.slice(1);
  const json = JSON.stringify(schema,null,2);
  // A Java-safe string literal, not a text block or expression evaluated from the prompt.
  const javaString = JSON.stringify(JSON.stringify(schema)).replace(/\\u2028|\\u2029/g,' ');
  return [
    {path:`schemas/${code}.json`,content:json+'\n'},
    {path:`apps/admin/src/generated/${code}/schema.ts`,content:`import type { PageSchema } from '@cms-agent/contracts';\nexport default ${json} satisfies PageSchema;\n`},
    {path:`apps/admin/src/generated/${code}/Page.vue`,content:`<script setup lang="ts">\nimport { CmsPage } from '@cms-agent/ui';\nimport schema from './schema';\n</script>\n<template><CmsPage :schema="schema" /></template>\n`},
    {path:`apps/admin/src/generated/${code}/route.ts`,content:`export default { path: '/generated/${code}', component: () => import('./Page.vue') };\n`},
    {path:`backend/src/main/java/com/cmsagent/generated/${name}Module.java`,content:`package com.cmsagent.generated;\n\nimport com.cmsagent.module.ModuleService;\nimport org.springframework.boot.ApplicationRunner;\nimport org.springframework.context.annotation.Bean;\nimport org.springframework.context.annotation.Configuration;\n\n/** Generated module: uses the platform's validated, persistent content API. */\n@Configuration\npublic class ${name}Module {\n    @Bean\n    ApplicationRunner register${name}(ModuleService modules) {\n        return args -> modules.installIfAbsent(${javaString});\n    }\n}\n`},
    {path:`docs/generated/${code}.md`,content:`# ${schema.title.replace(/[\r\n]/g,' ')}\n\nModule: \`${code}\`. Copy the apps/ and backend/ trees into CMS-Agent, then rebuild.\n\nThe backend registration exposes this module through \`/api/content/${code}\`. The existing sidebar discovers it automatically. The generated Vue route is optional; import its route into the router only when using a customized page.\n\nCRUD uses the shared content_entry table and JSON payload, with schema validation, pagination, session authentication and optimistic locking. This is NOT a per-module SQL table/entity generator or a standalone project.\n\nReview the schema and diff before applying. Existing modules are never overwritten; a conflicting schema causes startup to fail.\n`},
  ];
}
