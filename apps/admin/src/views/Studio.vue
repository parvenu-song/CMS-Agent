<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { plan, generate } from '@cms-agent/agent-core';
import { parseSchema } from '@cms-agent/contracts';
import { api } from '@cms-agent/api-client';
import { state, loadModules } from '../state';
const input=reactive({code:'news',title:'资讯管理',requirement:'创建一个资讯管理后台，包括标题、正文、作者、分类和状态，支持分页搜索、新增、编辑和删除。'});
const initial=plan(input),schemaText=ref(JSON.stringify(initial.schema,null,2)),warnings=ref(initial.warnings),engine=ref('规则模式');
const error=ref(''),busy=ref(false),activeTab=ref('preview'),selected=ref(0),router=useRouter();
const model=ref({enabled:false,model:''}),isAdmin=computed(()=>state.user?.roles.includes('ADMIN'));
const parsed=computed(()=>{try{return {schema:parseSchema(JSON.parse(schemaText.value)),error:''};}catch(e){return {schema:null,error:(e as Error).message};}});
const artifacts=computed(()=>parsed.value.schema?generate(parsed.value.schema):[]);
const columns=computed(()=>parsed.value.schema?.fields.filter(f=>f.list).map(f=>({title:f.label,dataIndex:f.name}))??[]);
const stages=['需求理解','规范校验','页面预览','模块创建'];
function rulePlan(){try{const p=plan(input);schemaText.value=JSON.stringify(p.schema,null,2);warnings.value=p.warnings;engine.value='规则模式';error.value='';selected.value=0;}catch(e){error.value=(e as Error).message;}}
async function llmPlan(){busy.value=true;error.value='';try{schemaText.value=JSON.stringify(parseSchema(await api.modelPlan(input)),null,2);engine.value=`模型 · ${model.value.model}`;warnings.value=['模型输出已做结构校验，但业务含义仍须人工审查。'];selected.value=0;}catch(e){error.value=(e as Error).message;}finally{busy.value=false;}}
async function create(){if(!parsed.value.schema)return;busy.value=true;error.value='';try{const m=await api.createModule(parsed.value.schema);await loadModules();message.success('模块已创建，可以开始管理内容');await router.push(`/content/${m.code}`);}catch(e){error.value=(e as Error).message;}finally{busy.value=false;}}
function download(){if(!parsed.value.schema)return;const blob=new Blob([JSON.stringify({version:1,schema:parsed.value.schema,files:artifacts.value},null,2)],{type:'application/json'});const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=`${parsed.value.schema.code}.bundle.json`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
onMounted(async()=>{try{model.value=await api.modelStatus();}catch{}});
</script>
<template>
  <section>
    <div class="section-heading"><div><span class="eyebrow">AGENT STUDIO</span><h1>把需求，变成内容平台。</h1><p>先定义规范，再生成页面。每一步都可预览、可审查。</p></div><a-tag color="purple">{{ engine }}</a-tag></div>
    <div class="hero-strip"><div><span class="hero-icon">✦</span><div><b>从重复搭建，到规范驱动</b><p>一份 Schema，连接 Ant Design Vue 页面和 Spring Boot 内容服务。</p></div></div><span class="hero-count"><b>{{ state.modules.length }}</b> 已创建模块</span></div>
    <div class="pipeline"><div v-for="(step,i) in stages" :key="step"><span>{{ String(i+1).padStart(2,'0') }}</span>{{ step }}<b v-if="i<3">→</b></div></div>
    <a-alert v-if="error" :message="error" type="error" show-icon style="margin-bottom:20px"/>
    <div class="studio-grid">
      <a-card title="描述你的页面" :bordered="false" class="request-card">
        <template #extra><span class="muted">01 / INPUT</span></template>
        <a-form layout="vertical">
          <div class="two-columns"><a-form-item label="模块标识"><a-input v-model:value="input.code" placeholder="news" :maxlength="32"/></a-form-item><a-form-item label="页面名称"><a-input v-model:value="input.title" :maxlength="80"/></a-form-item></div>
          <a-form-item label="业务需求"><a-textarea v-model:value="input.requirement" :rows="8" :maxlength="4000" show-count placeholder="描述字段、操作和业务规则…"/></a-form-item>
          <div class="prompt-hints"><span>支持的关键词</span><p>正文 · 摘要 · 作者 · 分类 · 价格 · 库存 · 排序 · 启用</p></div>
          <a-button type="primary" block size="large" :disabled="busy" @click="rulePlan">✦ 生成页面规范</a-button>
          <a-button v-if="model.enabled && isAdmin" block style="margin-top:12px" :loading="busy" @click="llmPlan">发送需求至模型规划</a-button>
          <p v-if="model.enabled" class="muted small">模型规划会向管理员配置的模型服务发送此处的需求文本。</p>
          <p v-else class="muted small">当前无需模型密钥。配置后端模型服务后，可启用模型规划。</p>
        </a-form>
      </a-card>
      <a-card :bordered="false" class="preview-card">
        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="preview" tab="页面预览">
            <template v-if="parsed.schema"><div class="preview-title"><b>{{ parsed.schema.title }}</b><a-tag>{{ parsed.schema.fields.length }} 个字段</a-tag></div><div class="preview-toolbar"><a-input disabled placeholder="搜索内容…" style="max-width:220px"/><a-button type="primary" disabled>＋ 新建</a-button></div><a-table size="small" :columns="columns" :data-source="[]" :pagination="false" :scroll="{x:480}"><template #emptyText><div class="preview-empty"><span>▤</span><b>页面结构已就绪</b><p>这是结构预览，不会写入数据。<br/>创建模块后，即可使用完整内容管理功能。</p></div></template></a-table><div class="field-tags"><a-tag v-for="f in parsed.schema.fields" :key="f.name">{{ f.label }} · {{ f.type }}{{ f.required?' *':'' }}</a-tag></div></template>
          </a-tab-pane>
          <a-tab-pane key="schema" tab="Schema 编辑"><p class="muted">修改字段后实时校验。非法字段与未知属性将被拒绝。</p><a-textarea v-model:value="schemaText" :rows="20" spellcheck="false" class="code-editor" :maxlength="65536"/></a-tab-pane>
          <a-tab-pane key="code" tab="生成代码"><a-select v-model:value="selected" style="width:100%;margin-bottom:12px" :options="artifacts.map((f,i)=>({label:f.path,value:i}))"/><pre class="code-preview">{{ artifacts[selected]?.content }}</pre></a-tab-pane>
        </a-tabs>
        <a-alert v-if="parsed.error" :message="parsed.error" type="error" show-icon/>
        <div class="preview-actions"><span class="muted">{{ artifacts.length }} 个待生成文件</span><a-space><a-button :disabled="!parsed.schema" @click="download">下载代码包 JSON</a-button><a-button type="primary" :disabled="!parsed.schema||!isAdmin" :loading="busy" @click="create">创建模块 →</a-button></a-space></div>
      </a-card>
    </div>
    <div class="guardrail"><b>◎ 生成边界</b><span v-for="warning in warnings" :key="warning">{{ warning }}</span><span>代码包包含平台内的 Vue 页面与 Java 模块注册器，不会自动修改其他仓库；审查后再通过 CLI 输出文件。</span></div>
  </section>
</template>
