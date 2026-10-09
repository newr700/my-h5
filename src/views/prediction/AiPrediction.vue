<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import TeamCrest from '@/components/TeamCrest.vue'
import RadarChart from './components/RadarChart.vue'
import { useAiPredictionStore } from '@/stores/aiPrediction'

/**
 * 页面4：AI 预测（草图中间那张）
 *
 * 布局重构（2026-10-09）：每支球队 = 一行横向卡片，三段式：
 *   · 左  概率  —— 沿用「概率前三名」那张卡片的样式（队徽 + 大数字 + 队名 + 概率条）
 *   · 中  雷达  —— 六维雷达图，鼠标悬浮才显示六维属性明细（默认只给形状，不挤数值）
 *   · 右  球星  —— 该队两张球星「照片」（有 photoUrl 显示图，没图显示队色占位卡）
 *
 * ── 为什么前端不自己算概率 ────────────────────────────────
 * 概率 = 六维加权求和，权重在后端 PredictionScoring 里集中定义。
 * 若前端也写一遍公式，改权重时就得同时改两处，漏一处就是「页面说的话和入口里的结果不一致」。
 * 所以前端只做【展示】，所有数字都是后端现算后下发的结果。
 */
const predictionStore = useAiPredictionStore()

/** 雷达图上是否显示数值（对应草图「要可调数字显示」） */
const showValue = ref(true)

const teams = computed(() => predictionStore.teams)

/**
 * 六维属性浮层：跟随鼠标移动显示（参考图那效果）。
 * 只记录「当前悬浮的是哪一队」+「鼠标在雷达图内的坐标」，
 * 坐标相对紧贴雷达图的 .ti-radar-hit 计算（不是外层 .ti-radar，那个范围偏大），
 * 浮层用 absolute 定位贴到鼠标旁。
 * 靠近容器右下边缘时翻折到鼠标另一侧，避免浮层被裁。
 */
const hoverRank = ref<number | null>(null)
const tip = reactive({ x: 0, y: 0, flipX: false, flipY: false })
/** 浮层大致尺寸，仅用于判断是否需要翻折（真实尺寸随内容略有出入，够用即可） */
const TIP_W = 176
const TIP_H = 150

function onRadarMove(e: MouseEvent, rank: number) {
  hoverRank.value = rank
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  tip.x = x
  tip.y = y
  tip.flipX = x + TIP_W > rect.width
  tip.flipY = y + TIP_H > rect.height
}
function onRadarLeave() {
  hoverRank.value = null
}

/** 概率条的颜色：越高越暖，前三名突出显示 */
function probColor(p: number): string {
  if (p >= 80) return '#e34d4d'
  if (p >= 60) return '#e6a23c'
  return '#409eff'
}

onMounted(() => {
  // 两支并行请求互不依赖，一起发即可（等一个再发另一个是没必要的串行等待）
  Promise.all([predictionStore.loadPredictions(), predictionStore.loadAlgorithm()]).catch(
    () => {
      /* 首屏错误已进 store.error，页面渲染错误态 */
    }
  )
})

function retry() {
  Promise.all([predictionStore.loadPredictions(), predictionStore.loadAlgorithm()]).catch(() => {
    ElMessage.error('仍然加载失败')
  })
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="title">这个算法分析英超</h2>
        <p class="subtitle">AI 模型评估每支球队的五个维度，并直接给出夺冠概率</p>
      </div>
      <!-- 数值开关：看图时数字会干扰整体形状，核对时又不能没有 -->
      <el-switch v-model="showValue" active-text="显示数值" inline-prompt />
    </div>

    <div v-if="predictionStore.loading" class="team-list">
      <div v-for="i in 4" :key="i" class="team-item skeleton">
        <el-skeleton animated>
          <template #template>
            <div class="ti-prob">
              <el-skeleton-item variant="circle" style="width: 44px; height: 44px" />
              <el-skeleton-item variant="p" style="width: 70%; margin-top: 12px" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <el-result v-else-if="predictionStore.error" icon="error" title="预测加载失败" :sub-title="predictionStore.error">
      <template #extra>
        <el-button type="primary" @click="retry">重试</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- ── 每支球队一项（横向）：左概率 / 中雷达(悬浮显六维) / 右两张球星照片 ── -->
      <div class="team-list">
        <div
          v-for="t in teams"
          :key="t.rank"
          class="team-item"
          :style="{ '--team-color': t.colorPrimary }"
        >
          <!-- 左：概率（沿用「概率前三名」那张卡片的样式） -->
          <div class="ti-prob">
            <TeamCrest
              :short-name="t.shortName"
              :color-primary="t.colorPrimary"
              :color-secondary="t.colorSecondary"
              :logo-url="t.logoUrl"
              :team-name="t.teamName"
              :size="40"
            />
            <div class="ti-prob-num" :style="{ color: probColor(t.winProbability) }">
              {{ t.winProbability }}<small>%</small>
            </div>
            <div class="ti-prob-name">{{ t.teamName }}</div>
            <el-progress
              :percentage="t.winProbability"
              :stroke-width="6"
              :color="probColor(t.winProbability)"
              :show-text="false"
            />
          </div>

          <!-- 中：雷达图；鼠标悬浮时六维属性以浮层跟随鼠标移动（参考图效果） -->
          <div class="ti-radar">
            <!-- 事件绑在紧贴雷达图的 .ti-radar-hit 上，而不是外层 .ti-radar：
                 .ti-radar 是 flex:1 的自适应容器，雷达图居中后两侧会留出空白，
                 绑在它上面会导致「鼠标还在卡片空白处就已经显示浮层」。
                 另外不能直接绑 <svg>——内联 SVG 未被绘制的空白区默认不接收鼠标事件，
                 那样只有精确指到网格线/多边形上才会触发，很难用。 -->
            <div
              class="ti-radar-hit"
              @mousemove="onRadarMove($event, t.rank)"
              @mouseleave="onRadarLeave"
            >
              <RadarChart :dims="t.dims" :color-primary="t.colorPrimary" :show-value="showValue" />
              <transition name="tip">
                <div
                  v-if="hoverRank === t.rank"
                  class="ti-dims"
                  :style="{
                    left: tip.x + 'px',
                    top: tip.y + 'px',
                    transform: tip.flipX
                      ? `translate(calc(-100% - 14px), ${tip.flipY ? 'calc(-100% - 12px)' : '12px'})`
                      : `translate(14px, ${tip.flipY ? 'calc(-100% - 12px)' : '12px'})`
                  }"
                >
                  <div class="ti-dims-title">{{ t.teamName }}</div>
                  <div v-for="d in t.dims" :key="d.key" class="ti-dim">
                    <span class="ti-dim-label">{{ d.label }}</span>
                    <span class="ti-dim-score">{{ d.score }}</span>
                  </div>
                </div>
              </transition>
            </div>
          </div>

          <!-- 右：球星照片（有图显示照片 + 球衣号角标；无图显示队色占位卡） -->
          <div class="ti-stars">
            <template v-if="t.starPlayers.length > 0">
              <div v-for="s in t.starPlayers.slice(0, 2)" :key="s.playerName" class="star-photo">
                <div class="star-frame">
                  <img v-if="s.photoUrl" :src="s.photoUrl" :alt="s.playerName" class="star-img" />
                  <div v-else class="star-card">
                    <span class="sc-num">{{ s.jerseyNumber }}</span>
                    <span class="sc-init">{{ s.playerName.charAt(0) }}</span>
                  </div>
                  <span v-if="s.jerseyNumber > 0" class="star-jersey">{{ s.jerseyNumber }}</span>
                </div>
                <div class="star-cap">
                  <span class="star-name">{{ s.playerName }}</span>
                  <span v-if="s.position" class="star-pos">{{ s.position }}</span>
                </div>
              </div>
            </template>
            <span v-else class="no-star">暂无球星资料</span>
          </div>

          <!-- 底部：AI 分析文字（文档给定；空串则不显示） -->
          <div v-if="t.analysis" class="ti-analysis">{{ t.analysis }}</div>
        </div>
      </div>

      <!-- ── 数据说明（原「算法说明与权重」，已改为说明数据来源）────────────── -->
        <h3 class="sec-title">数据说明</h3>
        <el-card shadow="never" class="algo">
          <p class="algo-desc">
            本页数据来自已训练的 AI 模型：对每支球队从下列 5 个维度评估打分（0~100），并直接输出夺冠概率。
            维度评分越高表示该队该项越强；其中「伤病风险」例外，评分越高表示伤病风险越大。
            夺冠概率由模型综合给出，并非各维度简单加权。
          </p>
          <div class="algo-weights">
            <div v-for="w in predictionStore.weights" :key="w.key" class="weight-chip">
              <span class="w-label">{{ w.label }}</span>
            </div>
          </div>
        </el-card>
    </template>
  </div>
</template>

<style scoped>
/* 建立堆叠上下文：让下方 ::before 那层背景（z-index:-1）
   正好压在 #app 的白底之上、页面内容之下 */
.page {
  position: relative;
  z-index: 0;

  /* 白色区域统一降透明度。
     Element Plus 的组件内部读这些变量，改这一处即可让
     卡片、进度条一起变透，不必逐个组件 :deep() 穿透。 */
  --el-card-bg-color: rgba(255, 255, 255, 0.78);
  --el-fill-color-blank: rgba(255, 255, 255, 0.72);
  --el-fill-color-lighter: rgba(255, 255, 255, 0.46);
}

/* ── 主题背景（AI 预测配色：浅绿）────────────────────────────
 * 固定定位的伪元素铺满视口当壁纸：不参与布局（原有间距不动）、
 * 锚定视口（本页有几十张雷达图，内容极高，随内容拉伸会把图糊掉）、
 * z-index:-1（永远在内容下面）。上面叠白色半透明遮罩保证可读性。 */
.page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)),
    url('../../assets/backgrounds/prediction.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.sec-title {
  font-size: 15px;
  margin: 22px 0 10px;
}
.title {
  margin: 0;
  font-size: 20px;
}
.subtitle {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

/* ── 横向列表：每支队一项，纵向堆叠、各占满整行 ── */
.team-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.team-item {
  display: flex;
  align-items: stretch;
  flex-wrap: wrap;
  gap: 18px;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  /* 沿用「概率前三名」卡片的视觉：顶部一道队色描边 + 半透明白底 */
  border-top: 3px solid var(--team-color, var(--el-color-primary));
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.78);
}

/* 左：概率（概率前三名卡片的样式） */
.ti-prob {
  flex: 0 0 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  text-align: center;
}
.ti-prob-num {
  font-size: 38px;
  font-weight: 700;
  line-height: 1.05;
}
.ti-prob-num small {
  font-size: 18px;
  margin-left: 2px;
}
.ti-prob-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.ti-prob :deep(.el-progress) {
  width: 100%;
  margin-top: 4px;
}

/* 中：雷达图 + 悬浮显示六维属性 */
.ti-radar {
  flex: 1 1 auto;
  position: relative;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 紧贴雷达图尺寸的「悬浮命中区」：宽度与 .radar 的 max-width 对齐，
   所以只有鼠标真正落在雷达图这块方块上才触发，两侧留白不算。
   同时它也是六维浮层的定位原点（before 是 .ti-radar 兼任，范围偏大）。 */
.ti-radar-hit {
  position: relative;
  width: 100%;
  max-width: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ti-radar :deep(.radar) {
  max-width: 240px;
  /* 悬浮轻微放大、离开回弹：给「这里可以看详情」的触感。
     用 transform 而不是改 width/max-width，不触发重排、周围元素不会被顶开。 */
  transition: transform 0.22s cubic-bezier(0.34, 1.16, 0.64, 1);
  transform-origin: center center;
}
.ti-radar-hit:hover :deep(.radar) {
  transform: scale(1.08);
}
/* 六维属性浮层：深蓝底的小卡片，跟随鼠标移动（参考图效果）。
   pointer-events:none 让鼠标"穿透"浮层，continue 触发 .ti-radar 的 mousemove，
   否则鼠标一进浮层就丢掉移动事件、浮层会卡住。 */
.ti-dims {
  position: absolute;
  z-index: 6;
  top: 0;
  left: 0;
  min-width: 150px;
  padding: 9px 12px;
  border-radius: 8px;
  background: rgba(23, 38, 66, 0.94);
  border: 1px solid rgba(96, 148, 224, 0.5);
  box-shadow: 0 10px 26px rgba(8, 18, 38, 0.38);
  color: #eaf1ff;
  pointer-events: none;
  will-change: transform, left, top;
}
.ti-dims-title {
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 5px;
  white-space: nowrap;
}
.ti-dim {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 12.5px;
  line-height: 1.75;
  white-space: nowrap;
}
.ti-dim-label {
  color: #b7c8e8;
}
.ti-dim-score {
  font-weight: 700;
  color: #7fc4ff;
}
/* 浮层出入场：仅淡入淡出；移动由 inline transform 实时驱动，不加过渡以免跟不上鼠标 */
.tip-enter-active,
.tip-leave-active {
  transition: opacity 0.15s ease;
}
.tip-enter-from,
.tip-leave-to {
  opacity: 0;
}

/* 右：球星照片 */
.ti-stars {
  flex: 0 0 180px;
  display: flex;
  gap: 10px;
}
.star-photo {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
/* 照片框：相对定位，作为球衣号角标的定位原点 */
.star-frame {
  position: relative;
  width: 100%;
}
.star-img {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  display: block;
  /* 照片上方压一层极淡暗角，保证角标与姓名在亮色照片上也清晰 */
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.06);
}
/* 球衣号角标：贴在照片左上角，像真实球衣号码牌 */
.star-jersey {
  position: absolute;
  top: 6px;
  left: 6px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  color: #fff;
  background: rgba(20, 28, 40, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 6px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
}
/* 无照片时的占位卡：队色渐变 + 球衣号 + 姓名首字，读起来像一张球员卡 */
.star-card {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.45);
  background: linear-gradient(160deg, var(--team-color, #409eff), rgba(20, 28, 40, 0.92));
}
.sc-num {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}
.sc-init {
  font-size: 22px;
  font-weight: 700;
}
.star-cap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.star-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.star-pos {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
.no-star {
  align-self: center;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

/* 底部 AI 分析文字：占满整行，与上方三列用虚线分隔 */
.ti-analysis {
  flex: 1 0 100%;
  margin-top: 2px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
}

/* 窄屏：三段竖排，避免横向挤爆 */
@media (max-width: 768px) {
  .team-item {
    flex-direction: column;
    align-items: stretch;
  }
  .ti-prob,
  .ti-stars {
    flex-basis: auto;
  }
}

.algo-desc {
  margin: 0 0 10px;
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.algo-formula {
  font-family: Consolas, Monaco, monospace;
  background: var(--el-fill-color-lighter);
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 12px;
  overflow-x: auto;
  white-space: nowrap;
}
.algo-weights {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.weight-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  font-size: 13px;
}
.weight-chip.total {
  font-weight: 700;
  border-color: var(--el-color-primary);
}
/* 合计不等于 100 时把这个标签标红，一眼看出配置有问题 */
.weight-chip.bad {
  border-color: var(--el-color-danger);
  color: var(--el-color-danger);
}
.w-label {
  color: var(--el-text-color-secondary);
}
.algo-warn {
  margin-top: 12px;
}
.algo-notes {
  margin: 12px 0 0;
  padding-left: 18px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.9;
}
.algo-notes code {
  background: var(--el-fill-color-lighter);
  padding: 1px 4px;
  border-radius: 3px;
}
</style>
