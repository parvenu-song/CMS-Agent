<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { api } from '@cms-agent/api-client';
import { state, clearSession } from './state';
const route=useRoute(),router=useRouter();
async function logout(){try{await api.logout();clearSession();await router.replace('/login');}catch(e){message.error((e as Error).message);}}
</script>
<template>
  <a-config-provider :theme="{token:{colorPrimary:'#635bff',borderRadius:10,fontFamily:'Inter, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif'}}">
    <router-view v-if="route.path==='/login'"/>
    <div v-else class="app-shell">
      <aside class="sidebar">
        <router-link to="/" class="brand"><span class="brand-mark">C<span>✦</span></span><div>CMS <b>Agent</b><small>BUILD WITH INTENT</small></div></router-link>
        <div class="workspace-label">工作空间 <span>V0.1</span></div>
        <nav>
          <router-link to="/" class="nav-item" exact-active-class="selected"><span>◈</span> Agent 工作台</router-link>
          <div class="nav-caption">内容管理</div>
          <router-link v-for="m in state.modules" :key="m.code" :to="`/content/${m.code}`" class="nav-item" active-class="selected"><span>▤</span>{{ m.title }}</router-link>
          <p v-if="!state.modules.length" class="sidebar-empty">创建模块后显示在这里</p>
          <div class="nav-caption">平台</div>
          <router-link v-if="state.user?.roles.includes('ADMIN')" to="/audit" class="nav-item" active-class="selected"><span>◷</span> 操作审计</router-link>
        </nav>
        <div class="sidebar-bottom"><span class="status-dot"></span> Vue + Spring Boot<small>Schema 是页面的单一规范</small></div>
      </aside>
      <main class="main-shell">
        <header class="topbar"><span>工作空间 <span class="muted">/</span> {{ route.path==='/'?'Agent 工作台':route.path==='/audit'?'操作审计':'内容管理' }}</span><div class="user-chip"><span class="avatar">{{ state.user?.username[0].toUpperCase() }}</span>{{ state.user?.username }}<a-button size="small" type="text" @click="logout">退出</a-button></div></header>
        <div class="page"><router-view/></div>
      </main>
    </div>
  </a-config-provider>
</template>
