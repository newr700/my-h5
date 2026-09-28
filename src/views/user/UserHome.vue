<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

// 页面只跟 store 打交道，不直接碰 api —— 依赖方向：views → stores → api
const userStore = useUserStore()

onMounted(() => {
  // 后端还没起时会请求失败，先吞掉错误展示空态
  userStore.loadProfile().catch(() => {})
})
</script>

<template>
  <div class="page">
    <van-nav-bar title="用户中心" />
    <template v-if="userStore.profile">
      <van-cell-group inset>
        <van-cell title="昵称" :value="userStore.profile.nickname" />
        <van-cell title="ID" :value="String(userStore.profile.id)" />
      </van-cell-group>
    </template>
    <van-empty v-else description="后端还没起，先看个壳" />
    <van-button type="primary" block class="reload-btn" @click="userStore.loadProfile">
      重新加载
    </van-button>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}
.reload-btn {
  margin: 16px;
  width: calc(100% - 32px);
}
</style>
