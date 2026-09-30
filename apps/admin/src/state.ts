import { reactive } from 'vue';
import { api } from '@cms-agent/api-client';
import type { PageSchema, User } from '@cms-agent/contracts';
export const state=reactive<{user:User|null;modules:PageSchema[]}>({user:null,modules:[]});
let initial:Promise<void>|undefined;
export function ensureSession(){return initial??=(async()=>{try{state.user=await api.me();await loadModules();}catch{state.user=null;}})();}
export async function loadModules(){state.modules=await api.modules();}
export function clearSession(){state.user=null;state.modules=[];initial=undefined;}
