<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '@cms-agent/api-client';
import { state, loadModules } from '../state';
const username=ref('admin'),password=ref(''),loading=ref(false),error=ref(''),router=useRouter();
async function submit(){loading.value=true;error.value='';try{state.user=await api.login(username.value,password.value);await loadModules();await router.replace('/');}catch(e){error.value=(e as Error).message;}finally{loading.value=false;}}
</script>
<template>
  <main class="login-screen"><section class="login-story"><div class="brand">CMS <b>Agent</b></div><span class="eyebrow">FROM IDEA TO INTERFACE</span><h1>让内容平台，<br/>从一个想法开始。</h1><p>把需求变成可审查的页面规范，<br/>再交给一致、可扩展的工程底座。</p><div class="login-stack">Vue 3 <span>＋</span> Monorepo <span>＋</span> Spring Boot</div></section>
    <section class="login-panel"><h2>进入工作空间</h2><p class="muted">使用管理员配置的账户登录</p><a-alert v-if="error" :message="error" type="error" show-icon style="margin:20px 0"/>
      <a-form layout="vertical" @finish="submit"><a-form-item label="用户名"><a-input v-model:value="username" size="large" autocomplete="username" required/></a-form-item><a-form-item label="密码"><a-input-password v-model:value="password" size="large" autocomplete="current-password" required/></a-form-item><a-button type="primary" html-type="submit" size="large" block :loading="loading" :disabled="!username||!password">登录工作空间 →</a-button></a-form>
      <p class="login-note">开发环境账户见 README。生产部署必须配置独立密码与 HTTPS。</p>
    </section>
  </main>
</template>
