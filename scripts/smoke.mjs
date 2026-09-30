// Black-box session + CSRF + CRUD test. Use a local dev/test instance only.
import assert from 'node:assert/strict';
const base=process.env.SMOKE_BASE_URL||'http://127.0.0.1:8080';
let cookies=new Map(),csrf;
async function call(url,options={}) {
  const headers=new Headers(options.headers);headers.set('Cookie',[...cookies].map(([k,v])=>`${k}=${v}`).join('; '));
  if(csrf && options.method && options.method!=='GET') headers.set(csrf.headerName,csrf.token);
  if(typeof options.body==='string')headers.set('Content-Type','application/json');
  const res=await fetch(base+url,{...options,headers,redirect:'manual',signal:AbortSignal.timeout(20000)});
  for(const cookie of res.headers.getSetCookie()) {const [part]=cookie.split(';'),i=part.indexOf('=');cookies.set(part.slice(0,i),part.slice(i+1));}
  return res;
}
async function token(){const res=await call('/api/auth/csrf');assert.equal(res.status,200);csrf=await res.json();}
assert.equal((await call('/api/modules')).status,401);
await token();
assert.equal((await call('/api/auth/login',{method:'POST',body:new URLSearchParams({username:'admin',password:process.env.SMOKE_ADMIN_PASSWORD||'dev-admin-change-me'})})).status,204);
await token();
assert.equal((await call('/api/auth/me')).status,200);
const code='smoke'+Date.now().toString(36);
const schema={version:1,code,title:'Smoke',fields:[{name:'title',label:'Title',type:'text',required:true,list:true}]};
assert.equal((await call('/api/modules',{method:'POST',body:JSON.stringify(schema)})).status,201);
let res=await call(`/api/content/${code}`,{method:'POST',body:JSON.stringify({data:{title:'Hello'}})});assert.equal(res.status,201);const entry=await res.json();
res=await call(`/api/content/${code}/${entry.id}`,{method:'PUT',body:JSON.stringify({data:{title:'Updated'},version:entry.version})});assert.equal(res.status,200);const updated=await res.json();
assert.equal((await call(`/api/content/${code}/${entry.id}`,{method:'DELETE'})).status,400);
assert.equal((await call(`/api/content/${code}/${entry.id}?version=${updated.version}`,{method:'DELETE'})).status,204);
assert.equal((await call('/api/auth/logout',{method:'POST'})).status,204);
assert.equal((await call('/api/modules')).status,401);
console.log('HTTP smoke passed: authentication, CSRF, create, update, delete, logout.');
