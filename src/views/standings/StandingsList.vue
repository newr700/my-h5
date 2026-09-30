<script setup lang="ts">
/**
 * ① 页面层：只负责「展示」和「用户交互」。
 * 数据从 store 拿（③），请求由 api 层发（②），这里不出现 axios。
 *
 * 页面结构按草图还原：
 *   加粗标题「当前积分排名」→ 加粗表头 → 三个分区（欧冠区橙色 / 欧联区 / 降级区）
 */
import { onMounted } from 'vue'
import { useStandingsStore } from '@/stores/standings'
import type { TeamStanding } from '@/types/api'

const standingsStore = useStandingsStore()

onMounted(() => {
  standingsStore.loadStandings()
})

/** 队徽占位：现在还没有真实队徽图片，用「排名数字圆圈」代替。
 *  以后拿到图片后，把这里换成 <img src="@/assets/images/xxx.png"> 即可 */
function rankBadgeClass(team: TeamStanding) {
  if (team.rank <= 6) return 'badge badge--ucl'
  if (team.rank <= 17) return 'badge badge--uel'
  return 'badge badge--rel'
}
</script>

<template>
  <div class="page">
    <!-- 加载失败 / 契约校验失败时显示，开发环境排查全靠它 -->
    <div v-if="standingsStore.error" class="alert">{{ standingsStore.error }}</div>

    <!-- 草图要求：标题加粗 -->
    <header class="title">当前积分排名</header>

    <!-- 草图要求：表头加粗 -->
    <div class="row row--header">
      <span class="col col--rank">排名</span>
      <span class="col col--team">球队</span>
      <span class="col col--num">轮次</span>
      <span class="col col--num col--wide">胜/平/负</span>
      <span class="col col--num">进失</span>
      <span class="col col--num">积分</span>
    </div>

    <!-- 分区一：欧冠区（1-6 名），草图标注这块要橙色 -->
    <div class="zone zone--ucl">欧冠区</div>
    <div v-for="team in standingsStore.championsLeagueZone" :key="team.rank" class="row">
      <span class="col col--rank"><i :class="rankBadgeClass(team)">{{ team.rank }}</i></span>
      <span class="col col--team">{{ team.teamName }}：当前积分{{ team.points }}</span>
      <span class="col col--num">{{ team.played }}</span>
      <span class="col col--num col--wide">{{ team.win }}/{{ team.draw }}/{{ team.lose }}</span>
      <span class="col col--num">{{ team.goalsFor }}/{{ team.goalsAgainst }}</span>
      <span class="col col--num col--points">{{ team.points }}</span>
    </div>

    <!-- 分区二：欧联区（7-17 名） -->
    <div class="zone zone--uel">欧联区</div>
    <div v-for="team in standingsStore.europaLeagueZone" :key="team.rank" class="row">
      <span class="col col--rank"><i :class="rankBadgeClass(team)">{{ team.rank }}</i></span>
      <span class="col col--team">{{ team.teamName }}</span>
      <span class="col col--num">{{ team.played }}</span>
      <span class="col col--num col--wide">{{ team.win }}/{{ team.draw }}/{{ team.lose }}</span>
      <span class="col col--num">{{ team.goalsFor }}/{{ team.goalsAgainst }}</span>
      <span class="col col--num col--points">{{ team.points }}</span>
    </div>

    <!-- 分区三：降级区（18-20 名） -->
    <div class="zone zone--rel">降级区</div>
    <div v-for="team in standingsStore.relegationZone" :key="team.rank" class="row">
      <span class="col col--rank"><i :class="rankBadgeClass(team)">{{ team.rank }}</i></span>
      <span class="col col--team">{{ team.teamName }}</span>
      <span class="col col--num">{{ team.played }}</span>
      <span class="col col--num col--wide">{{ team.win }}/{{ team.draw }}/{{ team.lose }}</span>
      <span class="col col--num">{{ team.goalsFor }}/{{ team.goalsAgainst }}</span>
      <span class="col col--num col--points">{{ team.points }}</span>
    </div>
  </div>
</template>

<style scoped>
/* 页面独有的样式就近写在这里，scoped 保证不影响别人。
   直接按 375 设计稿写 px，构建时自动转 vw。 */

.page {
  min-height: 100vh;
  background: #ffffff;
}

/* 错误提示条：契约校验失败时会在页面顶部显红 */
.alert {
  padding: 8px 12px;
  background: #fff1f0;
  color: #d43e3e;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.title {
  padding: 16px;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #ebedf0;
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

/* 队徽占位圆圈（草图左侧的「插图」位置） */
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

.badge--ucl {
  background: #ff976a; /* 欧冠区：草图标注的橙色 */
}

.badge--uel {
  background: #1989fa;
}

.badge--rel {
  background: #969799;
}

/* 分区标签条 */
.zone {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: bold;
  color: #ffffff;
}

.zone--ucl {
  background: #ff976a;
}

.zone--uel {
  background: #1989fa;
}

.zone--rel {
  background: #ee0a24;
}
</style>
