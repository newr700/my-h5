<template>
  <!--
    顶部导航栏（Element Plus 的 el-menu，mode="horizontal" 横向菜单）。
      - 左侧：品牌名 my-h5-app
      - 中间：6 个路由入口（router 模式，index 即路由 path，当前页自动高亮）
      - 空间不足时：放不下的入口收进「更多 ...」下拉（见下方 JS 的折叠计算）
      - 右侧：登录态 —— 已登录用 el-dropdown（头像+用户名，下拉可退出）；未登录显示「未登录」
      - 登录页 meta.nav===false 不显示导航栏（做全屏登录页）

    ── 为什么不用 Element Plus 自带的 ellipsis ─────────────────
    el-menu 的 :ellipsis 会把 el-menu 的【所有】子节点按宽度一起折叠，
    我们右侧的头像区(.nav-right)也是它的子节点，窄屏下会被塞进省略号里，
    就违背了「最右侧永远是用户头像/名称」的要求。所以改成自己算：
    只折叠菜单项，品牌名和头像区永远常驻。
  -->
  <el-menu
    v-if="showNav"
    ref="navRef"
    mode="horizontal"
    :default-active="route.path"
    router
    :ellipsis="false"
    class="app-nav"
  >
    <span ref="brandRef" class="brand">my-h5-app</span>

    <!-- 6 个入口：超出可视宽度的会被打上 .nav-hidden 隐藏，改由「更多」承接 -->
    <el-menu-item
      v-for="(item, i) in navItems"
      :key="item.path"
      :index="item.path"
      class="nav-item"
      :class="{ 'nav-hidden': i >= visibleCount }"
      :ref="(el: unknown) => setItemRef(el, i)"
    >
      {{ item.label }}
    </el-menu-item>

    <!-- 「更多」：装不下的入口。手机上没有 hover，所以用 click 触发的下拉而非 el-sub-menu -->
    <el-dropdown
      v-if="overflowItems.length > 0"
      class="nav-more"
      trigger="click"
      @command="(p: string) => router.push(p)"
    >
      <span class="nav-more-trigger" :class="{ 'is-active': activeInOverflow }">
        <el-icon><MoreFilled /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="item in overflowItems"
            :key="item.path"
            :command="item.path"
            :class="{ 'is-current': item.path === route.path }"
          >
            {{ item.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 右侧登录态：靠 margin-left:auto 推到最右 -->
    <div ref="rightRef" class="nav-right">
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, MoreFilled } from '@element-plus/icons-vue'
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

/* ───────────────────────────────────────────────────────────
   窄屏折叠（Priority+ 模式）：量出每个菜单项真实宽度，按剩余空间决定
   能放几个，放不下的收进「更多 ...」下拉。
   ─────────────────────────────────────────────────────────── */

/** 全部入口，顺序即展示顺序 */
const navItems = [
  { path: '/user', label: '我的' },
  { path: '/order', label: '购票' },
  { path: '/standings', label: '积分榜' },
  { path: '/analysis', label: '权威解析' },
  { path: '/prediction', label: 'AI 预测' },
  { path: '/history', label: '历史回顾' }
]

/** 「更多」按钮占用的宽度，要和 CSS 里 .nav-more-trigger 的尺寸对齐 */
const MORE_WIDTH = 44
/** 菜单区和右侧头像区之间留的呼吸位，避免文字贴到头像上 */
const SAFE_PAD = 8

const navRef = ref()
const brandRef = ref<HTMLElement>()
const rightRef = ref<HTMLElement>()
/** 每个菜单项的实际 DOM（el-menu-item 是组件，ref 给的是实例，取 $el） */
const itemEls: (HTMLElement | null)[] = []
function setItemRef(el: unknown, i: number) {
  const dom = (el as { $el?: HTMLElement } | null)?.$el
  itemEls[i] = dom instanceof HTMLElement ? dom : null
}

/** 当前能完整显示几个入口。初值给全部，好让首帧把它们都渲染出来测量宽度 */
const visibleCount = ref(navItems.length)
/** 测过的菜单项宽度缓存：内容是固定的，量一次就够，之后只按容器宽度重算个数 */
const cachedWidths: number[] = []

const overflowItems = computed(() => navItems.slice(visibleCount.value))
/** 当前页如果被折叠进「更多」，就让 ... 显示高亮，否则用户会以为自己不在任何页面 */
const activeInOverflow = computed(() => overflowItems.value.some((i) => i.path === route.path))

/**
 * 测量菜单项宽度。
 * 被隐藏（display:none）的项量出来是 0，这时沿用上一次测到的值 ——
 * 否则一旦折叠过，宽度缓存就被 0 覆盖，再拉宽窗口也算不出正确的个数。
 */
function measureItemWidths() {
  itemEls.forEach((el, i) => {
    if (!el) return
    const w = el.getBoundingClientRect().width
    if (w > 0.5) cachedWidths[i] = w
  })
}

/** 按当前可用宽度重算能显示几个；最后一项不必给「更多」留位置 */
function updateVisibleCount() {
  const nav = navRef.value?.$el as HTMLElement | undefined
  if (!nav) return
  const total = nav.getBoundingClientRect().width
  const brandW = brandRef.value?.getBoundingClientRect().width ?? 0
  const rightW = rightRef.value?.getBoundingClientRect().width ?? 0
  const available = total - brandW - rightW - SAFE_PAD

  let used = 0
  let count = 0
  for (let i = 0; i < cachedWidths.length; i++) {
    const isLastItem = i === cachedWidths.length - 1
    const reserve = isLastItem ? 0 : MORE_WIDTH
    if (used + (cachedWidths[i] ?? 0) + reserve <= available) {
      used += cachedWidths[i] ?? 0
      count++
    } else {
      break
    }
  }
  // 至少留住一个入口，别把整个导航都塞进下拉里
  visibleCount.value = Math.min(Math.max(count, 1), navItems.length)
}

let resizeObserver: ResizeObserver | null = null

onMounted(async () => {
  await nextTick()
  measureItemWidths()
  updateVisibleCount()
  // 字体可能晚于首帧加载完，那时文字宽度会变，就绪后再量一次修正
  document.fonts?.ready?.then(() => {
    measureItemWidths()
    updateVisibleCount()
  })
  // 视口变化（旋屏、拖动窗口）时按缓存宽度重算个数即可，不用重新测量
  const nav = navRef.value?.$el
  if (nav && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => updateVisibleCount())
    resizeObserver.observe(nav)
  }
})

onBeforeUnmount(() => resizeObserver?.disconnect())
</script>

<style scoped>
.app-nav {
  /* 背景图任务给每个页面的 .page 根元素加了 position:relative;z-index:0，
     会形成一个「高于普通文档流」的堆叠上下文，把固定定位的页面背景(::before)
     连带抬到顶部导航栏之上，导致导航栏被背景图盖住、看起来像透明。
     这里用 sticky 把导航栏钉在视口顶部、不随页面下拉而消失，并抬到更高层级 +
     给实底，恢复原本「不透明白色导航栏」且始终可见的观感。 */
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #fff;
  /* 入口从 3 个增到 6 个后，原先靠 flex-wrap:wrap 换行（会把内容整体顶下去、
     导航条占两行）。现在改为「装不下就折叠进更多」，所以恢复 Element Plus
     默认的 nowrap：一行显示 + 裁剪，具体显示几个由 JS 算。 */
  flex-wrap: nowrap;
  overflow: hidden;
}
/* 参与测量的节点都不许被 flex 压扁：一旦收缩，量到的宽度就不是真实宽度，
   折叠个数会算错（浏览器会先把它挤压到能塞下，我们就永远以为「放得下」）。 */
.brand,
.nav-right,
.nav-more,
.nav-item {
  flex-shrink: 0;
}
/* 超出可视宽度的入口：隐藏后由「更多」下拉承接 */
.nav-hidden {
  display: none;
}
/* el-menu 本身是 flex 容器，品牌名和右侧区都是它的子节点，可直接参与布局 */
.brand {
  font-weight: 700;
  font-size: 18px;
  padding: 16px 16px;
  white-space: nowrap;
}
/* 「更多」按钮：紧跟最后一个可见入口，右侧头像区由 margin-left:auto 顶到最右 */
.nav-more {
  display: flex;
  align-items: center;
}
.nav-more-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  outline: none;
  color: var(--el-text-color-regular);
}
.nav-more-trigger:hover,
.nav-more-trigger.is-active {
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}
/* 下拉里的当前页：加个勾边让「我在哪」在折叠状态下也看得见 */
.nav-more :deep(.el-dropdown-menu__item.is-current) {
  color: var(--el-color-primary);
  font-weight: 600;
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

/* ── 手机端：压缩留白，把宝贵的横向空間让给菜单项 ── */
@media (max-width: 640px) {
  .app-nav {
    /* 菜单项左右内边距从 20px 收到 12px，6 个入口能多显示一个 */
    --el-menu-base-level-padding: 12px;
    --el-menu-horizontal-height: 52px;
  }
  .brand {
    font-size: 15px;
    padding: 14px 10px;
  }
  .uname {
    /* 昵称过长时不挤菜单：超出省略，完整名字点头像下拉可见 */
    max-width: 84px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
/* 主流手机宽（iPhone SE ~14 Pro 都在 375~430 之间）再收一档：
   品牌名和左右留白各让出十几像素，往往就够多显示一个入口，
   少一次「点省略号才能找到」的操作。 */
@media (max-width: 430px) {
  .app-nav {
    --el-menu-base-level-padding: 10px;
  }
  .brand {
    font-size: 14px;
    padding: 12px 8px;
  }
  .uname {
    max-width: 64px;
  }
}
</style>
