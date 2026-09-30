<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { Audit } from '@cms-agent/contracts';
import { api } from '@cms-agent/api-client';
const rows=ref<Audit[]>([]),error=ref(''),loading=ref(false);
const columns=[{title:'时间',dataIndex:'createdAt'},{title:'操作者',dataIndex:'actor'},{title:'操作',dataIndex:'action'},{title:'模块',dataIndex:'moduleCode'},{title:'目标',dataIndex:'targetId'}];
async function load(){loading.value=true;error.value='';try{rows.value=await api.audit();}catch(e){error.value=(e as Error).message;}finally{loading.value=false;}}
onMounted(load);
</script>
<template><div class="section-heading"><div><span class="eyebrow">ACTIVITY LOG</span><h1>操作审计</h1><p>最近 100 次模块创建和内容变更。不记录内容正文或模型密钥。</p></div><a-button @click="load">刷新</a-button></div><a-alert v-if="error" type="error" :message="error"/><a-card :bordered="false"><a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" :scroll="{x:900}"/></a-card></template>
