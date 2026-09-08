<template>
  <div class="app-container">
    <page-header :title="title" />
    <el-alert v-if="type === 'COLLECT_CORRECTION'" title="审核期间原填报继续作为有效版本，更正通过后才更新报表。" type="info" show-icon :closable="false" class="mb8" />
    <el-card shadow="never" class="mb8">
      <el-form :inline="true"><el-form-item label="申请说明"><el-input v-model="form.reason" style="width:360px" maxlength="1000" /></el-form-item><el-form-item label="目标ID"><el-input v-model="form.targetId" style="width:220px" /></el-form-item><el-button type="primary" :disabled="!canAdd" @click="save">保存申请</el-button><el-button v-if="form.id" type="success" :loading="working" @click="submit">提交审批</el-button></el-form>
      <el-input v-model="form.afterJson" type="textarea" :rows="8" placeholder="供应商/填报请填写完整 JSON；代理请填写 principalId、agentId、deptId、type、node、startsAt、endsAt" />
    </el-card>
    <el-table v-loading="loading" :data="rows" border><el-table-column prop="id" label="申请ID" width="180" /><el-table-column prop="title" label="标题" min-width="220" /><el-table-column prop="status" label="状态" width="120" /><el-table-column prop="node" label="当前节点" width="130" /><el-table-column prop="revision" label="版本" width="80" /><el-table-column label="操作" width="220"><template #default="{ row }"><el-button link type="primary" @click="load(row.id)">查看</el-button><el-button v-if="row.canDecide" link type="success" @click="decide(row,true)">通过</el-button><el-button v-if="row.canDecide" link type="warning" @click="decide(row,false)">退回</el-button></template></el-table-column></el-table>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { cancelReview, decideReview, getReview, listReview, saveReview, submitReview, type ReviewCase } from '@/api/businessReview'
const route=useRoute(); const type=String(route.query.type || (route.path.includes('correction')?'COLLECT_CORRECTION':'SUPPLIER_CHANGE')); const title=computed(()=>({SUPPLIER_CHANGE:'供应商变更审批',DELEGATION:'长期代理授权',COLLECT_CORRECTION:'填报更正审批'} as Record<string,string>)[type] || '业务审批');
const rows=ref<ReviewCase[]>([]); const loading=ref(false); const working=ref(false); const canAdd=computed(()=>!form.id || ['DRAFT','RETURNED'].includes(form.status || 'DRAFT')); const form=reactive<ReviewCase>({type,reason:'',afterJson:'{}'});
function reload(){loading.value=true;listReview(type).then(r=>rows.value=r.rows || []).finally(()=>loading.value=false)}
function save(){saveReview({...form,type}).then(r=>{Object.assign(form,r.data || {});reload()})}
function load(id:string){getReview(id).then(r=>Object.assign(form,r.data?.request || {}))}
function key(){return `${Date.now()}-${Math.random().toString(36).slice(2)}`}
function submit(){if(!form.id)return;working.value=true;submitReview(form.id,{revision:form.revision || 0,key:key()}).then(r=>{Object.assign(form,r.data || {});reload()}).finally(()=>working.value=false)}
function decide(row:ReviewCase,approved:boolean){const comment=approved?'核对通过':'请补充资料后重新提交';decideReview(String(row.id),{revision:row.revision || 0,key:key(),comment,approved}).then(reload)}
onMounted(reload)
</script>
