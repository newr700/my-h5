<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import TeamCrest from '@/components/TeamCrest.vue'
import RadarChart from './components/RadarChart.vue'
import { usePredictionStore } from '@/stores/prediction'
import type { TeamPrediction } from '@/types/api'

/**
 * 页面4：AI 预测（草图中间那张）
 *
 * 草图的四条批注对应的实现：
 *   「这个表有 6 维」        → 下方的六维明细表（每支队一行，六个维度一列）
 *   「需要排序」            → 由后端按概率排好，前端【不再排序】（排序口径只留一处）
 *   「这个有夺冠概率」      → 顶部突出前三名的大数字
 *   「这个六边形要可调数字」 → 每个雷达图上的数值开关（showValue）
 *
 * ── 为什么前端不自己算概率 ────────────────────────────────
 * 概率 = 六维加权求和，权重在后端 PredictionScoring 里集中定义。
 * 若前端也写一遍公式，改权重时就得同时改两处，漏一处就是「页面说的话和入口里的结果不一致」。
 * 所以前端只做【展示】，所有数字都是后端现算后下发的结果。
 */
const predictionStore = usePredictionStore()

/** 雷达图上是否显示数值（对应草图「要可调数字显示」） */
const showValue = ref(true)

const teams = computed(() => predictionStore.teams)

/** 概率最高的前三 —— 草图把 87/85/83 三个数字单独画了出来，这里照做 */
const podium = computed(() => teams.value.slice(0, 3))

/** 表格的六维列：直接取第一支队携带的维度来决定表头，不写死六个名字 */
const dimColumns = computed(() => teams.value[0]?.dims ?? [])

/** 安全取值：某支队在某个维度上的分数（按 key 匹配，不依赖数组下标） */
function dimScore(row: TeamPrediction, key: string): number {
  return row.dims.find((d) => d.key === key)?.score ?? 0
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
        <p class="subtitle">六个维度加权，算出每支球队的夺冠概率</p>
      </div>
      <!-- 数值开关：看图时数字会干扰整体形状，核对时又不能没有 -->
      <el-switch v-model="showValue" active-text="显示数值" inline-prompt />
    </div>

    <div v-if="predictionStore.loading" class="pod">
      <el-skeleton v-for="i in 3" :key="i" animated class="pod-item">
        <template #template>
          <el-skeleton-item variant="circle" style="width: 48px; height: 48px" />
          <el-skeleton-item variant="p" style="width: 70%; margin-top: 12px" />
        </template>
      </el-skeleton>
    </div>

    <el-result v-else-if="predictionStore.error" icon="error" title="预测加载失败" :sub-title="predictionStore.error">
      <template #extra>
        <el-button type="primary" @click="retry">重试</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- ── 夺冠概率前三名（草图上的 87 / 85 / 83）───────────── -->
      <div class="pod">
        <div
          v-for="t in podium"
          :key="t.rank"
          class="pod-item"
          :style="{ '--team-color': t.colorPrimary }"
        >
          <TeamCrest
            :short-name="t.shortName"
            :color-primary="t.colorPrimary"
            :color-secondary="t.colorSecondary"
            :logo-url="t.logoUrl"
            :team-name="t.teamName"
            :size="40"
          />
          <div class="pod-prob" :style="{ color: probColor(t.winProbability) }">
            {{ t.winProbability }}<small>%</small>
          </div>
          <div class="pod-name">{{ t.teamName }}</div>
          <el-progress
            :percentage="t.winProbability"
            :stroke-width="6"
            :color="probColor(t.winProbability)"
            :show-text="false"
          />
        </div>
      </div>

      <!-- ── 六维明细表（草图「这个表有 6 维」）───────────────── -->
      <h3 class="sec-title">六维明细</h3>
      <el-table :data="teams" border stripe size="small" class="dim-table">
        <el-table-column prop="rank" label="排名" width="64" align="center" />
        <el-table-column label="球队" min-width="140">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.shortName"
                :color-primary="row.colorPrimary"
                :color-secondary="row.colorSecondary"
                :logo-url="row.logoUrl"
                :team-name="row.teamName"
                :size="22"
              />
              <span>{{ row.teamName }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 六个维度列由数据决定，不写死表头 -->
        <el-table-column
          v-for="dim in dimColumns"
          :key="dim.key"
          :label="dim.label"
          width="96"
          align="center"
        >
          <template #default="{ row }">{{ dimScore(row, dim.key) }}</template>
        </el-table-column>
        <el-table-column label="夺冠概率" width="110" align="center">
          <template #default="{ row }">
            <span class="prob-cell">{{ row.winProbability }}%</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- ── 每支队的六边形雷达图 ───────────────────────────── -->
      <h3 class="sec-title">能力雷达</h3>
      <div class="radar-grid">
        <div v-for="t in teams" :key="t.rank" class="radar-card">
          <div class="radar-head">
            <TeamCrest
              :short-name="t.shortName"
              :color-primary="t.colorPrimary"
              :color-secondary="t.colorSecondary"
              :logo-url="t.logoUrl"
              :team-name="t.teamName"
              :size="26"
            />
            <span class="radar-name">{{ t.teamName }}</span>
            <span class="radar-prob">{{ t.winProbability }}%</span>
          </div>
          <RadarChart
            :dims="t.dims"
            :color-primary="t.colorPrimary"
            :show-value="showValue"
          />
          <!-- 明星球员：可能是空数组，页面必须优雅显示空态而不是留一块白 -->
          <div class="stars">
            <template v-if="t.starPlayers.length > 0">
              <span v-for="s in t.starPlayers" :key="s.playerName" class="star">
                <el-tag size="small" effect="plain">{{ s.jerseyNumber }} {{ s.playerName }}</el-tag>
              </span>
            </template>
            <span v-else class="no-star">暂无球星资料</span>
          </div>
        </div>
      </div>

      <!-- ── 算法说明（草图底部「算法说明与权重」）────────────── -->
      <h3 class="sec-title">算法说明</h3>
      <el-card shadow="never" class="algo">
        <p class="algo-desc">
          每支球队的六个维度各给 0~100 分，按下列权重加权求和后换算成夺冠概率：
        </p>
        <div class="algo-formula">夺冠概率 = round( ( Σ(维度分 × 权重) + 50 ) / 100 )</div>
        <div class="algo-weights">
          <div v-for="w in predictionStore.weights" :key="w.key" class="weight-chip">
            <span class="w-label">{{ w.label }}</span>
            <span class="w-value">{{ w.score }}</span>
          </div>
          <!-- 权重合计应当正好 100；不等于 100 说明后端配错了，这里如实显示出来 -->
          <div class="weight-chip total" :class="{ bad: predictionStore.weightTotal !== 100 }">
            <span class="w-label">合计</span>
            <span class="w-value">{{ predictionStore.weightTotal }}</span>
          </div>
        </div>
        <el-alert
          v-if="predictionStore.weightTotal !== 100"
          type="warning"
          :closable="false"
          show-icon
          title="权重合计不等于 100，请检查后端配置"
          class="algo-warn"
        />
        <ul class="algo-notes">
          <li>
            概率与排名都由后端<b>现算</b>：数据库里只存六维原始分，
            不存「算好的概率」——存两份迟早会对不上。
          </li>
          <li>
            全程整数运算（不碰浮点），避免
            <code>0.15 + 0.2 + …</code> 累加后差 1 的经典误差。
          </li>
        </ul>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
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
/* 前三名：一行三列，窄屏自动折行 */
.pod {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.pod-item {
  text-align: center;
  padding: 16px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-top: 3px solid var(--team-color, var(--el-color-primary));
  border-radius: 8px;
  background: var(--el-bg-color);
}
.pod-prob {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.1;
  margin-top: 8px;
}
.pod-prob small {
  font-size: 16px;
  margin-left: 2px;
}
.pod-name {
  margin: 2px 0 8px;
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.dim-table {
  width: 100%;
}
.cell-team {
  display: flex;
  align-items: center;
  gap: 8px;
}
.prob-cell {
  font-weight: 600;
}
.radar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.radar-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-bg-color);
}
.radar-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.radar-name {
  font-size: 14px;
  font-weight: 600;
}
.radar-prob {
  margin-left: auto;
  font-weight: 700;
  color: var(--el-color-primary);
}
.stars {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  min-height: 24px;
}
.no-star {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
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
@media (max-width: 768px) {
  .pod {
    grid-template-columns: 1fr;
  }
}
</style>
