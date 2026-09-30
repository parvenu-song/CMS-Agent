import { createRouter, createWebHistory } from 'vue-router';
import { ensureSession, state, clearSession } from './state';
export const router=createRouter({history:createWebHistory(),routes:[
  {path:'/',component:()=>import('./views/Studio.vue')},
  {path:'/login',component:()=>import('./views/Login.vue')},
  {path:'/content/:code',component:()=>import('./views/Content.vue')},
  {path:'/audit',component:()=>import('./views/Audit.vue')},
  {path:'/:pathMatch(.*)*',redirect:'/'},
]});
router.beforeEach(async(to)=>{if(to.path==='/login')return;await ensureSession();if(!state.user)return '/login';if(to.path==='/audit'&&!state.user.roles.includes('ADMIN'))return '/';});
window.addEventListener('cms:unauthorized',()=>{clearSession();if(router.currentRoute.value.path!=='/login')void router.replace('/login');});
