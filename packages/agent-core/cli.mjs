#!/usr/bin/env node
import { readFile, writeFile, mkdir, lstat, realpath } from 'node:fs/promises';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { generate } from './src/index.ts';
const [command, input, ...args] = process.argv.slice(2);
async function inspect(root) {
  const base = await realpath(root || '.');
  const manifests = [];
  for (const relative of ['package.json','apps/admin/package.json','apps/web/package.json']) {
    const target = path.join(base,relative);
    try {
      const real = await realpath(target);
      if (!real.startsWith(base+path.sep)) throw new Error('拒绝读取仓库外的符号链接');
      const stat = await lstat(real); if (stat.size > 262144) throw new Error('package.json 过大');
      manifests.push(JSON.parse(await readFile(real,'utf8')));
    } catch (e) { if (e.code !== 'ENOENT') throw e; }
  }
  const dependencies = Object.assign({},...manifests.map(m=>({...m.dependencies,...m.devDependencies})));
  const vue = Boolean(dependencies.vue), react = Boolean(dependencies.react || dependencies.antd);
  console.log(JSON.stringify({root:base,framework:vue?'vue':react?'react':'unknown',
    adapter:vue && dependencies['ant-design-vue']?'ant-design-vue':'manual-review',
    manifestsRead:manifests.length,
    warnings:react?['React Ant Design 仓库不能直接套用 Vue 模板；不进行自动转换。']:['仅检查固定位置的 package.json；不是完整 AST／路由扫描。']},null,2));
}
async function output(schemaFile, out) {
  if (!schemaFile || !out) throw new Error('用法: pnpm agent generate <schema.json> --out <新目录>');
  const info=await lstat(schemaFile); if(info.size>524288) throw new Error('Schema 文件过大');
  const raw = JSON.parse(await readFile(schemaFile,'utf8'));
  // Ignore downloaded file paths/content; regenerate only from the validated schema.
  const files = generate(raw.schema ?? raw);
  const dest = path.resolve(out);
  // Refuse existing output roots (including symlinks): never silently overwrite a repository.
  await mkdir(path.dirname(dest),{recursive:true});
  await mkdir(dest,{recursive:false});
  for (const file of files) {
    const target=path.resolve(dest,file.path);
    if (!target.startsWith(dest+path.sep)) throw new Error('非法输出路径');
    await mkdir(path.dirname(target),{recursive:true});
    await writeFile(target,file.content,{flag:'wx'});
  }
  await writeFile(path.join(dest,'manifest.json'),JSON.stringify({version:1,files:files.map(f=>({path:f.path,sha256:createHash('sha256').update(f.content).digest('hex')}))},null,2),{flag:'wx'});
  console.log(`已生成 ${files.length} 个文件: ${dest}（尚未写入目标仓库）`);
}
try {
  if (command==='inspect') await inspect(input);
  else if(command==='generate' && args[0]==='--out') await output(input,args[1]);
  else throw new Error('用法: pnpm agent inspect <仓库目录> | pnpm agent generate <schema.json> --out <新目录>');
} catch(e) { console.error(e.message); process.exitCode=1; }
