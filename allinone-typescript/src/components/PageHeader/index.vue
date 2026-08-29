<template>
  <el-page-header class="app-page-header" @back="emit('back')">
    <template #content>
      <span class="page-header-title">{{ title }}</span>
    </template>
    <template v-if="$slots.extra" #extra>
      <slot name="extra" />
    </template>
  </el-page-header>
</template>

<script setup lang="ts" name="PageHeader">
defineProps<{
  title?: string
}>()

const emit = defineEmits<{
  back: []
}>()
</script>

<style scoped>
.app-page-header {
  margin-bottom: 16px;
}
.page-header-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
