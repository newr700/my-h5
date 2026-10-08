<script setup lang="ts">
/**
 * ① 页面层：只负责「展示」和「用户交互」。
 * 数据从 store 拿（③），请求由 api 层发（②），这里不出现 axios。
 *
 * ── 面试技能点（详见 docs/面试技能树.md T1/T2/T5）────────
 * 本文件：组合式 API / ref+computed / onMounted 生命周期 /
 *         v-if 与 v-for / :key 选择 / computed 缓存 / scoped CSS / Element Plus 组件
 *
 * ── 为什么页面不许发请求 ────────────────────────────────
 * 页面是整个项目里更换最频繁的文件（改版、加功能都先动它）。
 * 如果请求逻辑长在页面里，改版时就会顺手把业务逻辑一起丢掉；
 * 收在 api 层，页面随便换皮，数据管道原地不动。
 *
 * ── 本页对应的清单条目（改这个页面时先读一遍）────────────
 * [前端-列表三态]     加载中骨架屏 / 失败全屏重试 / 空态防御
 * [前端-交互]         刷新按钮（PC/手机通用，鼠标点击即可）；移动端下拉刷新已移除，
 *                    因为改用 Element Plus 桌面组件库，刷新入口统一为按钮
 * [前端-降级]         队徽有图显示图片，没图退化成排名圆圈
 */
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useStandingsStore } from '@/stores/standings'
import type { TeamStanding } from '@/types/api'

const standingsStore = useStandingsStore()

// ── 三态判定（读法：这个页面此刻长什么样，一眼能看出来）──────
// （技能点：computed 计算属性——依赖不变就不重算，有缓存；这是它和普通函数的区别）
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
// （技能点：生命周期 onMounted——DOM 挂载后才发请求，保证渲染和取数并行不阻塞）
// catch(() => {}) 不是吞错误：store 已经把原因写进 error 了，
// 这里只是告诉 Vue「我知道这个 Promise 会失败，别往控制台抛未处理告警」
onMounted(() => {
  standingsStore.loadStandings().catch(() => {})
})

/** 错误态的重试按钮：和首次加载是同一个动作，永远不要为重试另写一条加载逻辑 */
function onRetry() {
  standingsStore.loadStandings().catch(() => {})
}

// 刷新：失败时页面已经有数据了，只弹轻提示，绝不能把现有内容清掉。
// ── 为什么这里区分两种失败、上面首次失败却全屏报错？──────────────
// 因为错误提示的“音量”要和用户损失匹配：
// 首次失败用户两手空空 → 必须给重试入口（全屏错误）
// 刷新失败用户还在看旧数据 → 小声说一句就行（message），别打断人家
async function onRefresh() {
  try {
    await standingsStore.loadStandings()
    ElMessage.success('已更新')
  } catch {
    ElMessage.error('更新失败，请稍后再试')
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
      <div class="titlebar__side">
        <!-- 刷新按钮（PC/手机通用，鼠标点击即可）。
             改用 Element Plus 后不再有「触摸下拉刷新」的专属手势，
             统一用按钮刷新，简单且两端一致 -->
        <el-button
          class="refresh-btn"
          :loading="standingsStore.refreshing"
          :disabled="standingsStore.loading"
          @click="onRefresh"
        >
          刷新
        </el-button>
        <div class="legend">
          <span class="legend__item"><i class="dot dot--ucl" />欧冠区</span>
          <span class="legend__item"><i class="dot dot--uel" />欧联区</span>
          <span class="legend__item"><i class="dot dot--rel" />降级区</span>
        </div>
      </div>
    </header>

    <!-- 状态一：首次加载中 → 骨架屏。
         为什么用骨架屏不用转圈：它复刻了最终布局的形状，
         用户心理上觉得“内容已经在路上”而不是“在等一个未知的结果” -->
    <div v-if="isInitialLoading" class="skeleton">
      <el-skeleton v-for="n in 8" :key="n" class="skeleton__row" :rows="1" animated />
    </div>

    <!-- 状态二：首次加载失败 → 全屏错误 + 重试按钮 -->
    <el-result
      v-else-if="isInitialError"
      icon="error"
      :title="standingsStore.error"
    >
      <template #extra>
        <el-button type="primary" @click="onRetry">重新加载</el-button>
      </template>
    </el-result>

    <!-- 状态三（防御）：成功但没数据 → 空态 -->
    <el-empty v-else-if="isEmpty" description="赛季尚未开始，暂无积分数据" />

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
  </div>
</template>

<style scoped>
/* 页面独有的样式就近写在这里。
 *
 * ── 为什么用 scoped ───────────────────────────────────
 * Vue 会给这个组件的所有元素打上随机属性（如 data-v-1a2b3c），
 * 这些选择器只命中带该属性的元素——两个人写同名 class 互相覆盖的惨案就此绝迹。
 *
 * ── 现在直接写 px 就是真实像素 ─────────────────────────
 * 已移除 postcss-px-to-viewport，不再把 px 转 vw 等比缩放；
 * 改用 Element Plus 桌面组件库后，页面按真实像素书写，PC 上即所见尺寸，
 * 手机上偏小但可读（项目定位为 PC 为主、手机兼顾）。 */

.page {
  min-height: 100vh;
  background: #ffffff;
  padding: 0 24px;
}

/* 标题栏：左标题，右侧是「刷新按钮 + 图例」 */
.titlebar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 14px 0 10px;
  border-bottom: 1px solid #ebeef5;
  flex-wrap: wrap;
  gap: 8px;
}

.titlebar__side {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
}

/* 分区图例：让人知道三种颜色各代表什么 */
.legend {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: #606266;
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

.dot--ucl { background: #e6a23c; }
.dot--uel { background: #409eff; }
.dot--rel { background: #f56c6c; }

/* 骨架屏行间距 */
.skeleton__row {
  padding: 12px 0;
  border-bottom: 1px solid #f2f6fc;
}

.row {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f2f6fc;
  font-size: 13px;
}

.row--header {
  font-weight: bold;
  background: #f5f7fa;
  /* 表头是列表最顶一行：只圆「左右上角」，与下方内容自然衔接 */
  border-radius: 8px 8px 0 0;
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

.badge--ucl { background: #e6a23c; } /* 欧冠区 */
.badge--uel { background: #409eff; }
.badge--rel { background: #909399; }

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

.zone--ucl { background: #e6a23c; }
.zone--uel { background: #409eff; }
.zone--rel { background: #f56c6c; }

/* ══ PC / 平板断点（≥ 768px）════════════════════════════════════
 *
 * ── 技能点：响应式设计（面试高频）──────────────────────────
 * 「移动端优先」：默认样式写手机，再用 min-width 向上覆盖。
 * 为什么不用 max-width 向下覆盖？反过来写的话，手机端要下载并计算一堆
 * 它永远用不上的 PC 规则 —— 手机流量和算力都更贵。
 *
 * 现在既然不再有 vw 等比缩放，这套覆盖就是「把手机尺寸放大到桌面舒适值」，
 * 而不是「救场防止 5 倍放大」。
 */
@media (min-width: 768px) {
  .page {
    padding: 0 48px;
  }

  .titlebar {
    padding: 20px 0 16px;
  }

  .title {
    font-size: 24px;
  }

  .subtitle {
    font-size: 14px;
  }

  .legend {
    gap: 16px;
    font-size: 13px;
  }

  .dot {
    width: 10px;
    height: 10px;
  }

  /* hover 是 PC 独有的交互态：手指没有「悬停」这个概念。
     放在断点外的后果是手机端点完一行，高亮残留在屏幕上不走。 */
  .row:hover {
    background: #f5f7fa;
  }

  /* 列宽同步放大：字号变大后，原来的 40px 排名列装不下队徽 */
  .col--rank {
    width: 56px;
  }

  .col--num {
    width: 72px;
  }

  .col--wide {
    width: 110px;
  }

  .badge {
    width: 32px;
    height: 32px;
    font-size: 15px;
  }

  .badge-img {
    width: 32px;
    height: 32px;
  }

  .zone {
    padding: 8px 12px;
    font-size: 13px;
  }

  .row {
    font-size: 15px;
  }
}
</style>
