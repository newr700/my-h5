<template>
  <!--
    顶部导航栏（Element Plus 的 el-menu，mode="horizontal" 横向菜单）。
    设计成「常规 Web 导航栏」样式，PC 为主、手机兼顾：
      - 左侧：品牌名 my-h5-app
      - 菜单：我的 / 购票 / 积分榜（router 模式，index 即路由 path，当前页自动高亮）
      - 右侧：登录态 —— 已登录用 el-dropdown（头像+用户名，下拉可退出）；未登录显示「未登录」
      - 登录页 meta.nav===false 不显示导航栏（做全屏登录页）
  -->
  <el-menu
    v-if="showNav"
    mode="horizontal"
    :default-active="route.path"
    router
    :ellipsis="false"
    class="app-nav"
  >
    <span class="brand">my-h5-app</span>

    <el-menu-item index="/user">我的</el-menu-item>
    <el-menu-item index="/order">购票</el-menu-item>
    <el-menu-item index="/standings">积分榜</el-menu-item>
    <!-- V7 新增：草图三张页面。同样走 router 模式，当前页自动高亮 -->
    <el-menu-item index="/analysis">权威解析</el-menu-item>
    <el-menu-item index="/prediction">AI 预测</el-menu-item>
    <el-menu-item index="/history">历史回顾</el-menu-item>

    <!-- 右侧登录态：靠 margin-left:auto 推到最右 -->
    <div class="nav-right">
      <el-dropdown v-if="userStore.isLoggedIn" @command="onCommand">
        <span class="user-trigger">
          <!-- 有头像就显示图片；没有就显示首字母占位（V6 头像上传后才有 avatarUrl） -->
          <el-avatar :size="28" :src="avatarImgSrc" v-if="avatarImgSrc" />
          <el-avatar :size="28" v-else>{{ avatarText }}</el-avatar>
          <span class="uname">{{ displayName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">用户中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button v-else type="primary" size="small" @click="router.push('/login')">未登录</el-button>
    </div>
  </el-menu>
  <!-- 内容区：统一加内边距防贴边；铺满视口由 responsive.css 的 #app 决定 -->
  <div class="app-main">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { avatarSrc } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 登录页 meta.nav===false 不显示导航栏
const showNav = computed(() => route.meta.nav !== false)

// 用户名首字符作为头像占位（后端有 avatar 字段后换成图片更专业）
const displayName = computed(
  () => userStore.profile?.nickname || userStore.profile?.username || '已登录'
)
const avatarText = computed(() => (displayName.value || '?').charAt(0).toUpperCase())
// 头像是相对路径（如 /uploads/12_xxx.png），按部署环境拼成可访问地址；
// 没有头像时返回 ''，模板用 v-if 切回首字母占位
const avatarImgSrc = computed(() => avatarSrc(userStore.profile?.avatarUrl))

// 应用启动 / 刷新页面后：若本地有 token 则拉资料恢复登录态，
// 否则刷新后 store 重建、profile 为 null，右上角又会显示「未登录」
onMounted(() => {
  userStore.restore()
})

function onCommand(command: string) {
  if (command === 'profile') {
    router.push('/user')
  } else if (command === 'logout') {
    userStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
/* 入口从 3 个增到 6 个：窄屏允许换行，避免菜单项被挤扁或溢出容器。
   宁可占两行，也不让用户以为某个入口不存在 */
.app-nav {
  flex-wrap: wrap;
  /* 背景图任务给每个页面的 .page 根元素加了 position:relative;z-index:0，
     会形成一个「高于普通文档流」的堆叠上下文，把固定定位的页面背景(::before)
     连带抬到顶部导航栏之上，导致导航栏被背景图盖住、看起来像透明。
     这里用 sticky 把导航栏钉在视口顶部、不随页面下拉而消失，并抬到更高层级 +
     给实底，恢复原本「不透明白色导航栏」且始终可见的观感。 */
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #fff;
}
/* el-menu 本身是 flex 容器，品牌名和右侧区都是它的子节点，可直接参与布局 */
.brand {
  font-weight: 700;
  font-size: 18px;
  padding: 16px 16px;
  white-space: nowrap;
}
/* 把登录态整个块推到最右侧 */
.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  padding-right: 12px;
}
/* 头像+用户名+箭头 作为下拉触发器，鼠标悬停有手型提示（PC 礼仪） */
.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}
.uname {
  font-size: 14px;
}
</style>
