<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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

// 头像占位文字：昵称/用户名首字符（后端补 avatar 字段后可换图）
const avatarText = computed(() => {
  const name = userStore.profile?.nickname || userStore.profile?.username || '?'
  return name.charAt(0).toUpperCase()
})

function onLogout() {
  // 退出登录用二次确认：这是不可逆操作（清掉本地登录态），误触成本高，
  // 用 ElMessageBox.confirm 拦一道，比直接登出更稳妥（技能点：危险操作二次确认）
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      userStore.logout()
      // 登出后回登录页（replace 不留历史，按返回键不会回到要登录的页面）
      router.replace('/login')
    })
    .catch(() => {
      // 用户点「取消」也会进 catch，这是 ElMessageBox 取消的正常路径，不是错误
    })
}
</script>

<template>
  <div class="page">
    <!-- 资料加载成功：卡片 + 描述列表 + 操作 -->
    <el-card v-if="userStore.profile" class="profile-card">
      <template #header>
        <div class="card-header">
          <el-avatar :size="44">{{ avatarText }}</el-avatar>
          <span class="header-title">用户中心</span>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="昵称">{{ userStore.profile.nickname }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ userStore.profile.username }}</el-descriptions-item>
        <el-descriptions-item label="ID">{{ userStore.profile.id }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ userStore.profile.createdAt }}</el-descriptions-item>
      </el-descriptions>

      <div class="actions">
        <el-button type="primary" @click="router.push('/order')">去看看我的订单</el-button>
        <el-button @click="onLogout">退出登录</el-button>
      </div>
    </el-card>

    <!-- 加载中：骨架屏 -->
    <el-card v-else-if="userStore.loading" class="state-card">
      <el-skeleton :rows="4" animated />
    </el-card>

    <!-- 加载失败：错误态 + 重试入口（清单：错误必须可感知） -->
    <el-card v-else class="state-card">
      <el-result icon="error" title="资料加载失败">
        <template #extra>
          <el-button type="primary" @click="userStore.loadProfile">重试</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
}
.profile-card,
.state-card {
  max-width: 600px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
