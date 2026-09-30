export type FieldType = 'text' | 'textarea' | 'number' | 'boolean' | 'select';
export type Scalar = string | number | boolean | null;
export interface Field {
  name: string; label: string; type: FieldType; required: boolean;
  list: boolean; options?: string[];
}
export interface PageSchema { version: 1; code: string; title: string; fields: Field[] }
export interface Entry {
  id: string; version: number; data: Record<string, Scalar>;
  createdAt: string; updatedAt: string;
}
export interface Page<T> { items: T[]; total: number; page: number; size: number }
export interface User { username: string; roles: string[] }
export interface Audit { id: string; actor: string; action: string; moduleCode: string; targetId: string; createdAt: string }
const forbidden = new Set(['constructor','prototype','__proto__','id','version','createdAt','updatedAt']);
const types = new Set<FieldType>(['text','textarea','number','boolean','select']);
function object(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('必须是 JSON 对象');
  return value as Record<string, unknown>;
}
function keys(value: Record<string, unknown>, allowed: string[]) {
  for (const key of Object.keys(value)) if (!allowed.includes(key)) throw new Error(`不支持的属性: ${key}`);
}
function text(value: unknown, max: number): string {
  if (typeof value !== 'string' || !value.trim() || value.length > max) throw new Error(`文本长度必须为 1–${max}`);
  return value;
}
/** Validate untrusted schema before rendering, persistence or code generation. */
export function parseSchema(raw: unknown): PageSchema {
  const s = object(raw); keys(s, ['version','code','title','fields']);
  if (s.version !== 1) throw new Error('仅支持 Schema version 1');
  const code = text(s.code, 32);
  if (!/^[a-z][a-z0-9]{1,31}$/.test(code)) throw new Error('模块标识须为 2–32 位小写字母、数字，字母开头');
  const title = text(s.title, 80);
  if (!Array.isArray(s.fields) || s.fields.length < 1 || s.fields.length > 30) throw new Error('字段数量必须为 1–30');
  const seen = new Set<string>();
  const fields = s.fields.map((rawField): Field => {
    const f = object(rawField); keys(f, ['name','label','type','required','list','options']);
    const name = text(f.name,32);
    if (!/^[a-z][a-zA-Z0-9]{0,31}$/.test(name) || forbidden.has(name) || seen.has(name)) throw new Error(`非法、保留或重复字段: ${name}`);
    seen.add(name);
    if (!types.has(f.type as FieldType)) throw new Error(`不支持的字段类型: ${String(f.type)}`);
    if (typeof f.required !== 'boolean' || typeof f.list !== 'boolean') throw new Error('required / list 必须为布尔值');
    const field: Field = {name, label:text(f.label,80),type:f.type as FieldType,required:f.required,list:f.list};
    if (f.type === 'select') {
      if (!Array.isArray(f.options) || !f.options.length || f.options.length > 20) throw new Error('选项数量必须为 1–20');
      field.options = f.options.map(v=>text(v,80));
      if (new Set(field.options).size !== field.options.length) throw new Error('选项不能重复');
    } else if (f.options !== undefined && f.options !== null) throw new Error('只有 select 支持 options');
    return field;
  });
  if (!fields.some(f => f.list)) throw new Error('至少一个字段必须显示在列表中');
  return {version:1,code,title,fields};
}
export function validateData(schema: PageSchema, raw: unknown): Record<string, Scalar> {
  const data = object(raw); keys(data, schema.fields.map(f=>f.name));
  const result: Record<string, Scalar> = {};
  for (const f of schema.fields) {
    const v = data[f.name];
    if (v === undefined || v === null || (typeof v === 'string' && !v.trim())) {
      if (f.required) throw new Error(`${f.label}不能为空`);
      result[f.name] = null; continue;
    }
    if (f.type === 'boolean') { if (typeof v !== 'boolean') throw new Error(`${f.label}必须是布尔值`); }
    else if (f.type === 'number') { if (typeof v !== 'number' || !Number.isFinite(v) || Math.abs(v) > 1e12) throw new Error(`${f.label}必须是绝对值不大于 10¹² 的数字`); }
    else {
      if (typeof v !== 'string' || v.length > (f.type === 'textarea' ? 20000 : 200)) throw new Error(`${f.label}类型错误或文本过长`);
      if (f.type === 'select' && !f.options?.includes(v)) throw new Error(`${f.label}的选项无效`);
    }
    result[f.name] = v as Scalar;
  }
  return result;
}
