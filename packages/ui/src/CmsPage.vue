<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import type { Entry, PageSchema, Scalar } from '@cms-agent/contracts';
import { validateData } from '@cms-agent/contracts';
import { api } from '@cms-agent/api-client';
const props=defineProps<{schema:PageSchema}>();
const entries=ref<Entry[]>([]),total=ref(0),page=ref(0),q=ref(''),loading=ref(false),saving=ref(false),error=ref('');
const open=ref(false),editing=ref<Entry>(),canWrite=ref(false),canDelete=ref(false);
const form=reactive<Record<string,Scalar>>({});let generation=0;
const columns=computed(()=>[...props.schema.fields.filter(f=>f.list).map(f=>({title:f.label,dataIndex:['data',f.name],key:f.name,ellipsis:true})),{title:'操作',key:'actions',width:150}]);
async function load(){
  const current=++generation;loading.value=true;error.value='';
  try{const result=await api.entries(props.schema.code,page.value,q.value);if(current!==generation)return;entries.value=result.items;total.value=result.total;}
  catch(e){if(current===generation)error.value=(e as Error).message;}
  finally{if(current===generation)loading.value=false;}
}
function edit(entry?:Entry){
  editing.value=entry;Object.keys(form).forEach(k=>delete form[k]);
  for(const f of props.schema.fields)form[f.name]=entry?.data[f.name] ?? (f.type==='boolean'?false:null);
  open.value=true;
}
async function save(){
  saving.value=true;
  try{await api.save(props.schema.code,validateData(props.schema,form),editing.value);open.value=false;message.success('保存成功');await load();}
  catch(e){message.error((e as Error).message);}finally{saving.value=false;}
}
async function remove(entry:Entry){
  try{await api.remove(props.schema.code,entry);if(entries.value.length===1&&page.value>0)page.value--;await load();message.success('已删除');}
  catch(e){message.error((e as Error).message);}
}
function search(){page.value=0;void load();}
watch(()=>props.schema.code,()=>{page.value=0;q.value='';open.value=false;entries.value=[];total.value=0;void load();});
onMounted(async()=>{try{const u=await api.me();canWrite.value=u.roles.some(r=>r==='ADMIN'||r==='EDITOR');canDelete.value=u.roles.includes('ADMIN');}catch{}await load();});
</script>
<template>
  <section>
    <div class="section-heading"><div><span class="eyebrow">CONTENT COLLECTION</span><h1>{{ schema.title }}</h1><p>标准化列表、表单与服务端校验 · /{{ schema.code }}</p></div><a-button v-if="canWrite" type="primary" size="large" @click="edit()">＋ 新建内容</a-button></div>
    <a-card :bordered="false">
      <div class="table-toolbar"><a-input-search v-model:value="q" placeholder="搜索内容…" :maxlength="100" style="max-width:340px" allow-clear @search="search"/><a-button @click="load">刷新</a-button><span class="muted">共 {{ total }} 条</span></div>
      <a-alert v-if="error" :message="error" type="error" show-icon style="margin-bottom:16px"/>
      <a-table :columns="columns" :data-source="entries" :loading="loading" row-key="id" :scroll="{x:700}" :pagination="{current:page+1,pageSize:10,total,showSizeChanger:false}" @change="(p:any)=>{page=(p.current??1)-1;load()}">
        <template #bodyCell="{column,record,text}">
          <a-space v-if="column.key==='actions'"><a-button v-if="canWrite" type="link" size="small" @click="edit(record as Entry)">编辑</a-button><a-popconfirm v-if="canDelete" title="删除后无法恢复，确认删除？" @confirm="remove(record as Entry)"><a-button type="link" danger size="small">删除</a-button></a-popconfirm></a-space>
          <a-tag v-else-if="column.key==='status'">{{ text }}</a-tag><span v-else>{{ typeof text==='boolean'?(text?'是':'否'):(text??'—') }}</span>
        </template>
        <template #emptyText><div style="padding:40px">还没有内容，从创建第一条开始。</div></template>
      </a-table>
    </a-card>
    <a-modal v-model:open="open" :title="editing?'编辑内容':'新建内容'" :confirm-loading="saving" :mask-closable="false" @ok="save" ok-text="保存" cancel-text="取消">
      <a-form layout="vertical" style="margin-top:24px" @submit.prevent="save">
        <a-form-item v-for="f in schema.fields" :key="f.name" :label="f.label" :required="f.required">
          <a-textarea v-if="f.type==='textarea'" v-model:value="form[f.name] as string" :rows="5" :maxlength="20000"/>
          <a-input-number v-else-if="f.type==='number'" v-model:value="form[f.name] as number" :min="-1e12" :max="1e12" style="width:100%"/>
          <a-switch v-else-if="f.type==='boolean'" v-model:checked="form[f.name] as boolean"/>
          <a-select v-else-if="f.type==='select'" v-model:value="form[f.name] as string" :options="f.options?.map(v=>({label:v,value:v}))" allow-clear/>
          <a-input v-else v-model:value="form[f.name] as string" :maxlength="200"/>
        </a-form-item>
      </a-form>
    </a-modal>
  </section>
</template>
