<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 用户中心（PRD-F8）。
 * 路由守卫保证进入本页时已有 token；这里负责把 token 换成真实资料。
 */
const userStore = useUserStore()
const router = useRouter()

onMounted(() => {
  // token 过期/被删时会触发 request.ts 的全局 401 处理（自动跳登录），
  // 所以这里静默 catch 即可，不用重复处理跳转
  userStore.loadProfile().catch(() => {})
})

function onLogout() {
  userStore.logout()
  // 登出后回登录页（hash 路由；replace 不留历史，按返回键不会回到要登录的页面）
  router.replace('/login')
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="用户中心" />

    <template v-if="userStore.profile">
      <van-cell-group inset>
        <van-cell title="昵称" :value="userStore.profile.nickname" />
        <van-cell title="用户名" :value="userStore.profile.username" />
        <van-cell title="ID" :value="String(userStore.profile.id)" />
        <van-cell title="注册时间" :value="userStore.profile.createdAt" />
      </van-cell-group>

      <div class="action-area">
        <van-button type="primary" block round @click="router.push('/order')">
          去看看我的订单
        </van-button>
        <van-button block round class="logout-btn" @click="onLogout">
          退出登录
        </van-button>
      </div>
    </template>

    <van-empty v-else-if="userStore.loading" description="加载中…" />
    <van-empty v-else description="资料加载失败">
      <van-button type="primary" round @click="userStore.loadProfile">重试</van-button>
    </van-empty>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}
.action-area {
  margin: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.logout-btn {
  color: #ee0a24;
}

@media (min-width: 768px) {
  .action-area {
    max-width: 320px;
    margin: 24px auto;
  }
}
</style>
