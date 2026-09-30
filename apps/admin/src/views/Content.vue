<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { CmsPage } from '@cms-agent/ui';
import { state } from '../state';
const route=useRoute(),schema=computed(()=>state.modules.find(m=>m.code===route.params.code));
</script>
<template><CmsPage v-if="schema" :schema="schema"/><a-result v-else status="404" title="模块不存在" sub-title="请返回工作台创建模块，或刷新页面。"/></template>
