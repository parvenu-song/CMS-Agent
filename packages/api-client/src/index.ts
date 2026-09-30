import type { PageSchema, Entry, Page, User, Audit } from '@cms-agent/contracts';
let csrf: {token:string;headerName:string}|undefined;
export class ApiError extends Error { constructor(public status:number,message:string){super(message);} }
export async function refreshCsrf() {
  const res=await fetch('/api/auth/csrf',{credentials:'same-origin'});
  if(!res.ok)throw new ApiError(res.status,'无法获取安全令牌，请确认后端已启动');
  csrf=await res.json();
}
export async function request<T>(path:string, init:RequestInit={}):Promise<T> {
  const headers=new Headers(init.headers),method=init.method ?? 'GET';
  if(!['GET','HEAD','OPTIONS'].includes(method.toUpperCase())) {
    if(!csrf)await refreshCsrf();
    headers.set(csrf!.headerName,csrf!.token);
  }
  if(init.body && !(init.body instanceof URLSearchParams))headers.set('Content-Type','application/json');
  let response:Response;
  try { response=await fetch(path,{...init,headers,credentials:'same-origin',signal:init.signal ?? AbortSignal.timeout(45000)}); }
  catch {throw new ApiError(0,'请求失败或超时，请检查后端连接');}
  if(!response.ok) {
    const body=await response.json().catch(()=>({}));
    if(response.status===401)window.dispatchEvent(new Event('cms:unauthorized'));
    throw new ApiError(response.status,body.message || ({401:'请重新登录',403:'权限不足或安全令牌过期',409:'数据已被修改，请刷新后重试'} as Record<number,string>)[response.status] || `请求失败 (${response.status})`);
  }
  return response.status===204 ? undefined as T : response.json();
}
export const api={
  me:()=>request<User>('/api/auth/me'),
  async login(username:string,password:string){await refreshCsrf();await request('/api/auth/login',{method:'POST',body:new URLSearchParams({username,password})});await refreshCsrf();return this.me();},
  async logout(){await request('/api/auth/logout',{method:'POST'});csrf=undefined;},
  modules:()=>request<PageSchema[]>('/api/modules'),
  createModule:(schema:PageSchema)=>request<PageSchema>('/api/modules',{method:'POST',body:JSON.stringify(schema)}),
  entries:(code:string,page=0,q='')=>request<Page<Entry>>(`/api/content/${encodeURIComponent(code)}?page=${page}&size=10&q=${encodeURIComponent(q)}`),
  save:(code:string,data:unknown,entry?:Entry)=>request<Entry>(`/api/content/${encodeURIComponent(code)}${entry?'/'+entry.id:''}`,{method:entry?'PUT':'POST',body:JSON.stringify({data,...(entry?{version:entry.version}:{})})}),
  remove:(code:string,entry:Entry)=>request<void>(`/api/content/${encodeURIComponent(code)}/${entry.id}?version=${entry.version}`,{method:'DELETE'}),
  audit:()=>request<Audit[]>('/api/audit'),
  modelStatus:()=>request<{enabled:boolean;model:string}>('/api/agent/status'),
  modelPlan:(input:{code:string;title:string;requirement:string})=>request<PageSchema>('/api/agent/plan',{method:'POST',body:JSON.stringify(input)}),
};
