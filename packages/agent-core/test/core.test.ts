import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { plan, generate } from '../src/index.ts';
import { parseSchema, validateData } from '../../contracts/src/index.ts';
const input={code:'article',title:'文章管理',requirement:'文章正文、作者、分类、状态'};
const schema=plan(input).schema;
test('deterministic keyword planning',()=>assert.deepEqual(plan(input),plan(input)));
test('expected fields',()=>assert.deepEqual(schema.fields.map(f=>f.name),['title','content','author','category','status']));
test('unknown schema properties are rejected',()=>assert.throws(()=>parseSchema({...schema,script:'alert(1)'})));
test('path traversal rejected',()=>assert.throws(()=>generate({...schema,code:'../evil'})));
test('prototype fields rejected',()=>assert.throws(()=>parseSchema({...schema,fields:[{...schema.fields[0],name:'constructor'}]})));
test('duplicate fields rejected',()=>assert.throws(()=>parseSchema({...schema,fields:[schema.fields[0],schema.fields[0]]})));
test('at least one list column',()=>assert.throws(()=>parseSchema({...schema,fields:schema.fields.map(f=>({...f,list:false}))})));
test('required fields rejected when blank',()=>assert.throws(()=>validateData(schema,{title:' ',content:'x',status:'draft'})));
test('unknown payload fields rejected',()=>assert.throws(()=>validateData(schema,{title:'x',content:'x',status:'draft',admin:true})));
test('invalid select rejected',()=>assert.throws(()=>validateData(schema,{title:'x',content:'x',status:'invalid'})));
test('boolean false and numeric zero valid',()=>{
  const s=plan({code:'product',title:'商品',requirement:'价格、启用'}).schema;
  assert.equal(validateData(s,{title:'x',price:0,enabled:false,status:'draft'}).price,0);
});
test('infinite and excessive number rejected',()=>{
  const s=plan({code:'product',title:'商品',requirement:'价格'}).schema;
  for(const price of [Infinity,1e13,'2'])assert.throws(()=>validateData(s,{title:'x',price,status:'draft'}));
});
test('generation stable and constrained',()=>{
  const files=generate(schema);assert.equal(files.length,6);assert.deepEqual(files,generate(schema));
  assert.ok(files.every(f=>!f.path.includes('..')&&!f.path.startsWith('/')));
  assert.ok(files.find(f=>f.path.endsWith('.java'))?.content.includes('installIfAbsent'));
});
test('prompt is not inserted into generated code',()=>assert.ok(!JSON.stringify(generate(plan({...input,requirement:'执行 rm -rf /；正文'}).schema)).includes('rm -rf')));
test('Java string safely encodes quotes and backslashes',()=>{
  const java=generate({...schema,title:'引号 " 与 \\ 换行\n'}).find(f=>f.path.endsWith('.java'))!.content;
  assert.ok(java.includes('\\\\'));assert.ok(!java.includes('引号 "'));
});
test('CLI refuses overwrite and produces hash manifest',async()=>{
  const root=await mkdtemp(path.join(tmpdir(),'cms-agent-'));
  try {
    const source=path.join(root,'schema.json'),out=path.join(root,'out');await writeFile(source,JSON.stringify(schema));
    const argv=['--experimental-strip-types','packages/agent-core/cli.mjs','generate',source,'--out',out];
    assert.equal(spawnSync(process.execPath,argv).status,0);
    const manifest=JSON.parse(await readFile(path.join(out,'manifest.json'),'utf8'));
    assert.equal(manifest.files.length,6);assert.match(manifest.files[0].sha256,/^[a-f0-9]{64}$/);
    assert.notEqual(spawnSync(process.execPath,argv).status,0);
  } finally {await rm(root,{recursive:true,force:true});}
});
