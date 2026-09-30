<script setup lang="ts">
/**
 * ① 页面层：只负责「展示」和「用户交互」。
 * 数据从 store 拿（③），请求由 api 层发（②），这里不出现 axios。
 * ── 为什么页面不许发请求 ────────────────────────────────
 * 页面是整个项目里更换最频繁的文件（改版、加功能都先动它）。
 * 如果请求逻辑长在页面里，改版时就会顺手把业务逻辑一起丢掉；
 * 收在 api 层，页面随便换皮，数据管道原地不动。
 *
 * ── 本页对应的清单条目（改这个页面时先读一遍）────────────
 * [前端-列表三态]     加载中骨架屏 / 失败全屏重试 / 空态防御
 * [前端-交互]         下拉刷新（移动端标配），刷新失败轻提示不打断浏览
 * [前端-降级]         队徽有图显示图片，没图退化成排名圆圈
 */
import { computed, onMounted } from 'vue'
import { showFailToast, showSuccessToast } from 'vant'
import { useStandingsStore } from '@/stores/standings'
import type { TeamStanding } from '@/types/api'

const standingsStore = useStandingsStore()

// ── 三态判定（读法：这个页面此刻长什么样，一眼能看出来）──────
/** 状态一：首次加载中 —— 页面一条数据都没有，正在拉取 → 骨架屏 */
const isInitialLoading = computed(
  () => standingsStore.loading && standingsStore.standings.length === 0
)
/** 状态二：首次加载失败 —— 没数据可用且有错误原因 → 全屏错误 + 重试 */
const isInitialError = computed(
  () => standingsStore.error !== '' && standingsStore.standings.length === 0
)
/** 状态三（防御性）：请求成功但数据为空 → 空态。
 *  本页因 expectCount 强制 20 条，正常走不到这里；
 *  但其他列表页（订单、消息）很可能返回空数组，这个分支必须留着当模板。 */
const isEmpty = computed(
  () => !standingsStore.loading && !standingsStore.error && standingsStore.standings.length === 0
)

// 首次进入：失败不用弹 toast——错误会占据整个屏幕，没人看不见。
// catch(() => {}) 不是吞错误：store 已经把原因写进 error 了，
// 这里只是告诉 Vue「我知道这个 Promise 会失败，别往控制台抛未处理告警」
onMounted(() => {
  standingsStore.loadStandings().catch(() => {})
})

/** 错误态的重试按钮：和首次加载是同一个动作，永远不要为重试另写一条加载逻辑 */
function onRetry() {
  standingsStore.loadStandings().catch(() => {})
}

// 下拉刷新：失败时页面已经有数据了，只弹轻提示，绝不能把现有内容清掉。
// ── 为什么这里区分两种失败、上面首次失败却全屏报错？──────────────
// 因为错误提示的“音量”要和用户损失匹配：
// 首次失败用户两手空空 → 必须给重试入口（全屏错误）
// 刷新失败用户还在看旧数据 → 小声说一句就行（toast），别打断人家
async function onRefresh() {
  try {
    await standingsStore.loadStandings()
    showSuccessToast('已更新')
  } catch {
    showFailToast('更新失败，请稍后再试')
  }
}

/**
 * 分区配置：数据驱动渲染，以后加减分区只改这个数组。
 * ── 为什么不直接在 template 里写三段 ─────────────────────
 * 三段几乎相同的 HTML 是“重复代码”的典型味道；
 * 抽成配置后，页面结构（v-for 一段）和数据（这个数组）分离，
 * 联赛改成 18 队、加个“争冠组”，动的只有这里。
 */
const zones = computed(() => [
  { key: 'ucl', title: '欧冠区', teams: standingsStore.championsLeagueZone },
  { key: 'uel', title: '欧联区', teams: standingsStore.europaLeagueZone },
  { key: 'rel', title: '降级区', teams: standingsStore.relegationZone }
])

/** 队徽降级策略：没图时用「排名数字圆圈」，颜色跟随分区 */
function rankBadgeClass(team: TeamStanding) {
  if (team.rank <= 6) return 'badge badge--ucl'
  if (team.rank <= 17) return 'badge badge--uel'
  return 'badge badge--rel'
}
</script>

<template>
  <div class="page">
    <!-- 草图要求：标题加粗；副标题和图例是对照清单补的「信息层级」 -->
    <header class="titlebar">
      <div>
        <h1 class="title">当前积分排名</h1>
        <p v-if="standingsStore.roundSummary" class="subtitle">{{ standingsStore.roundSummary }}</p>
      </div>
      <div class="legend">
        <span class="legend__item"><i class="dot dot--ucl" />欧冠区</span>
        <span class="legend__item"><i class="dot dot--uel" />欧联区</span>
        <span class="legend__item"><i class="dot dot--rel" />降级区</span>
      </div>
    </header>

    <!-- 下拉刷新包在最外层：移动端用户刷新列表的肌肉记忆就是这个手势。
         disabled 条件：首次加载时禁用——内容都没有，没有可“刷新”的东西 -->
    <van-pull-refresh
      v-model="standingsStore.refreshing"
      :disabled="standingsStore.loading"
      @refresh="onRefresh"
    >
      <!-- 状态一：首次加载中 → 骨架屏。
           为什么用骨架屏不用转圈：它复刻了最终布局的形状，
           用户心理上觉得“内容已经在路上”而不是“在等一个未知的结果” -->
      <div v-if="isInitialLoading" class="skeleton">
        <van-skeleton
          v-for="n in 8"
          :key="n"
          class="skeleton__row"
          title
          avatar
          :row="1"
          row-width="60%"
        />
      </div>

      <!-- 状态二：首次加载失败 → 全屏错误 + 重试按钮 -->
      <van-empty v-else-if="isInitialError" image="error" :description="standingsStore.error">
        <van-button round type="primary" size="small" @click="onRetry">重新加载</van-button>
      </van-empty>

      <!-- 状态三（防御）：成功但没数据 → 空态 -->
      <van-empty v-else-if="isEmpty" description="赛季尚未开始，暂无积分数据" />

      <!-- 状态四：正常数据 -->
      <template v-else>
        <!-- 草图要求：表头加粗 -->
        <div class="row row--header">
          <span class="col col--rank">排名</span>
          <span class="col col--team">球队</span>
          <span class="col col--num">轮次</span>
          <span class="col col--num col--wide">胜/平/负</span>
          <span class="col col--num">进失</span>
          <span class="col col--num">积分</span>
        </div>

        <template v-for="zone in zones" :key="zone.key">
          <div class="zone" :class="`zone--${zone.key}`">{{ zone.title }}</div>
          <!-- :key 用 rank：它是这条数据的天然唯一编号；
               千万别用数组下标当 key——排序变化时 Vue 会复用错节点，页面闪烁串行 -->
          <div v-for="team in zone.teams" :key="team.rank" class="row">
            <span class="col col--rank">
              <!-- 队徽降级：有图显示图，没图显示排名圆圈 -->
              <img v-if="team.logoUrl" class="badge-img" :src="team.logoUrl" :alt="team.teamName" />
              <i v-else :class="rankBadgeClass(team)">{{ team.rank }}</i>
            </span>
            <span class="col col--team">{{ team.teamName }}</span>
            <span class="col col--num">{{ team.played }}</span>
            <span class="col col--num col--wide">{{ team.win }}/{{ team.draw }}/{{ team.lose }}</span>
            <span class="col col--num">{{ team.goalsFor }}/{{ team.goalsAgainst }}</span>
            <span class="col col--num col--points">{{ team.points }}</span>
          </div>
        </template>
      </template>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
/* 页面独有的样式就近写在这里。
 *
 * ── 为什么用 scoped ───────────────────────────────────
 * Vue 会给这个组件的所有元素打上随机属性（如 data-v-1a2b3c），
 * 这些选择器只命中带该属性的元素——两个人写同名 class 互相覆盖的惨案就此绝迹。
 *
 * ── 为什么敢直接写 px ─────────────────────────────────
 * 构建时 postcss-px-to-viewport 会把 px 自动换算成 vw：
 * 375px 设计稿上写 13px，在 750px 宽的安卓机上渲染出来还是占同样的比例。
 * 所以“按设计稿的像素值抄”就是这个项目的正确写法。 */

.page {
  min-height: 100vh;
  background: #ffffff;
}

/* 标题栏：左标题右图例 */
.titlebar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 14px 12px 10px;
  border-bottom: 1px solid #ebedf0;
}

.title {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #969799;
}

/* 分区图例：让人知道三种颜色各代表什么 */
.legend {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: #646566;
}

.legend__item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot--ucl { background: #ff976a; }
.dot--uel { background: #1989fa; }
.dot--rel { background: #ee0a24; }

/* 骨架屏行间距 */
.skeleton__row {
  padding: 12px;
  border-bottom: 1px solid #f7f8fa;
}

.row {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f2f3f5;
  font-size: 13px;
}

.row--header {
  font-weight: bold;
  background: #f7f8fa;
}

/* 列宽：排名和球队占左边，四个数字列等宽对齐 */
.col--rank {
  width: 40px;
  flex-shrink: 0;
}

.col--team {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.col--num {
  width: 42px;
  flex-shrink: 0;
  text-align: center;
}

.col--wide {
  width: 62px;
}

.col--points {
  font-weight: bold;
}

/* 队徽占位圆圈（没图时的降级形态）。
   宽高固定 24px：图片位是“坑位”，坑位稳定，后面的文字列才不会左右跳 */
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-style: normal;
  color: #ffffff;
}

.badge--ucl { background: #ff976a; } /* 欧冠区：草图标注的橙色 */
.badge--uel { background: #1989fa; }
.badge--rel { background: #969799; }

/* 真队徽图片（后端补了 logoUrl 字段后自动生效） */
.badge-img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: contain;
}

/* 分区标签条 */
.zone {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: bold;
  color: #ffffff;
}

.zone--ucl { background: #ff976a; }
.zone--uel { background: #1989fa; }
.zone--rel { background: #ee0a24; }
</style>
