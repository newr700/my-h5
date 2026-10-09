<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import TeamCrest from '@/components/TeamCrest.vue'
import { useHistoryStore } from '@/stores/prediction'

/**
 * 页面5：历史回顾（草图最后一张）
 *
 * 上半部「历届战绩」：届数 / 赛季 / 冠军 / 亚军 / 季军 / 殿军，按届数倒序（最新在最前）。
 * 下半部「英超历届夺冠次数」：全时期（1992-93 起），由 season_history 现算。
 *
 * ── 赛季标签为什么不在这里拼 ──────────────────────────────
 * 接口直接给了 seasonLabel（如 "2024-25"）。
 * 让前端拿 seasonYear=2024 自己拼也可以，但那样「怎么显示赛季」这件事
 * 就散落在每个用到它的地方了 —— 展示格式属于契约，应该由后端统一给出。
 */
const historyStore = useHistoryStore()

const seasons = computed(() => historyStore.seasons)
const titles = computed(() => historyStore.titles)

/** 柱状图按最大次数归一化：保证第一名占满，其余按比例，视觉上才可比 */
const maxCount = computed(() => Math.max(1, ...titles.value.map((t) => t.count)))
function barWidth(count: number): string {
  return `${(count / maxCount.value) * 100}%`
}

onMounted(() => {
  Promise.all([historyStore.loadSeasons(), historyStore.loadTitleCounts()]).catch(() => {
    /* 首屏错误已进 store.error */
  })
})

function retry() {
  Promise.all([historyStore.loadSeasons(), historyStore.loadTitleCounts()]).catch(() => {
    ElMessage.error('仍然加载失败')
  })
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="title">历史回顾</h2>
        <p class="subtitle">
          {{ historyStore.editionRange ? `历届战绩（${historyStore.editionRange}）` : '历届战绩' }}
        </p>
      </div>
    </div>

    <div v-if="historyStore.loading">
      <el-skeleton animated :rows="6" />
    </div>

    <el-result v-else-if="historyStore.error" icon="error" title="战绩加载失败" :sub-title="historyStore.error">
      <template #extra>
        <el-button type="primary" @click="retry">重试</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- ── 历届战绩 ─────────────────────────────────────── -->
      <el-table :data="seasons" border stripe size="small" height="420" class="hist-table">
        <el-table-column prop="edition" label="届数" width="80" align="center" />
        <el-table-column prop="seasonLabel" label="赛季" width="100" align="center" />
        <el-table-column label="冠军" min-width="150">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.champion.shortName"
                :color-primary="row.champion.colorPrimary"
                :color-secondary="row.champion.colorSecondary"
                :logo-url="row.champion.logoUrl"
                :team-name="row.champion.teamName"
                :size="22"
              />
              <span class="champion">{{ row.champion.teamName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="亚军" min-width="140">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.runnerUp.shortName"
                :color-primary="row.runnerUp.colorPrimary"
                :color-secondary="row.runnerUp.colorSecondary"
                :logo-url="row.runnerUp.logoUrl"
                :team-name="row.runnerUp.teamName"
                :size="22"
              />
              <span>{{ row.runnerUp.teamName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="季军" min-width="140">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.third.shortName"
                :color-primary="row.third.colorPrimary"
                :color-secondary="row.third.colorSecondary"
                :logo-url="row.third.logoUrl"
                :team-name="row.third.teamName"
                :size="22"
              />
              <span>{{ row.third.teamName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="殿军" min-width="140">
          <template #default="{ row }">
            <div class="cell-team">
              <TeamCrest
                :short-name="row.fourth.shortName"
                :color-primary="row.fourth.colorPrimary"
                :color-secondary="row.fourth.colorSecondary"
                :logo-url="row.fourth.logoUrl"
                :team-name="row.fourth.teamName"
                :size="22"
              />
              <span>{{ row.fourth.teamName }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- ── 英超历届夺冠次数（现算，追加赛季自动更新）────── -->
      <h3 class="sec-title">英超历届夺冠次数（自 1992-93 起）</h3>
      <el-card shadow="never" class="title-card">
        <div v-for="(item, i) in titles" :key="item.team.teamName" class="bar-row">
          <span class="bar-rank">{{ i + 1 }}</span>
          <TeamCrest
            :short-name="item.team.shortName"
            :color-primary="item.team.colorPrimary"
            :color-secondary="item.team.colorSecondary"
            :logo-url="item.team.logoUrl"
            :team-name="item.team.teamName"
            :size="24"
          />
          <span class="bar-name">{{ item.team.teamName }}</span>
          <span class="bar-track">
            <span
              class="bar-fill"
              :style="{
                width: barWidth(item.count),
                background: item.team.colorPrimary
              }"
            />
          </span>
          <span class="bar-count">{{ item.count }}</span>
        </div>
        <p class="bar-note">
          由 season_history 现算（GROUP BY + COUNT），不单独维护统计字段 ——
          追加新赛季后本榜自动更新。统计口径为全时期（1992-93 起）。
        </p>
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

  /* 白色区域统一降透明度（Element Plus 组件内部读这些变量）。
     表格单独一组：表格的「白」分散在行 / 表头 / 斑马纹三个变量里，
     只改一个会得到「表头透了、数据行还是白的」这种半吊子效果。 */
  --el-card-bg-color: rgba(255, 255, 255, 0.78);
  --el-fill-color-blank: rgba(255, 255, 255, 0.72);
  --el-fill-color-lighter: rgba(255, 255, 255, 0.46);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: rgba(255, 255, 255, 0.62);
  --el-table-header-bg-color: rgba(255, 255, 255, 0.82);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.95);
}

/* ── 主题背景（历史回顾配色：黄绿）────────────────────────────
 * 固定定位的伪元素铺满视口当壁纸：不参与布局（原有间距不动）、
 * 锚定视口（这张表可以滚很久，随内容拉伸会把图糊掉）、
 * z-index:-1（永远在内容下面）。上面叠白色半透明遮罩保证可读性。 */
.page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)),
    url('../../assets/backgrounds/history.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.page-head {
  margin-bottom: 16px;
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
.sec-title {
  font-size: 15px;
  margin: 22px 0 10px;
}
.hist-table {
  width: 100%;
}
.cell-team {
  display: flex;
  align-items: center;
  gap: 8px;
}
/* 冠军加粗强调：一张战绩表里最重要的一列 */
.champion {
  font-weight: 600;
}
.title-card {
  padding: 4px 0;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.bar-rank {
  width: 20px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.bar-name {
  width: 84px;
  font-size: 14px;
  white-space: nowrap;
}
/* 轨道撑满剩余空间，填充条在其中按归一化比例展开 */
.bar-track {
  flex: 1;
  height: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  overflow: hidden;
  min-width: 80px;
}
.bar-fill {
  display: block;
  height: 100%;
  border-radius: 6px;
  transition: width 0.4s ease;
}
.bar-count {
  width: 32px;
  text-align: right;
  font-weight: 700;
  font-size: 15px;
}
.bar-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
</style>
