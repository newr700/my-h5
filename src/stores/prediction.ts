import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  fetchAlgorithm,
  fetchAnalysisComments,
  fetchExpertAnalysis,
  fetchPredictions,
  fetchSeasonHistory,
  fetchTitleCounts,
  postExpertComment
} from '@/api/prediction'
import type {
  DimScore,
  ExpertAnalysis,
  ExpertComment,
  SeasonHistory,
  TeamPrediction,
  TitleCount
} from '@/types/api'

/**
 * 英超预测模块的状态（三个 store 放在一个文件里）
 *
 * ── 为什么不分三个文件 ────────────────────────────────────
 * 项目的规矩是「按业务模块分文件」，而这三张页面本来就同属 prediction 一个后端模块：
 * 接口在同一个 Controller 下、数据来自同一批迁移脚本。
 * 拆成三个文件只会让人多点三次開editor，并没有减少耦合 —— 结构应该反映真实边界，而不是页面数量。
 *
 * ── 三态约定（沿用 stores/standings.ts）────────────────────
 *   加载中 loading     首次进入，一条数据都没有 → 骨架屏
 *   刷新中 refreshing  已有数据，正在更新 → 不打断浏览
 *   加载失败 error     只有「一条数据都没有」时才占据全屏
 * 失败信息永远如实展示 —— 不塞假数据让页面假装很热闹。
 */

/** 把「首次/刷新」这套重复逻辑收成一个：三个 store 都要用它 */
function useLoadState() {
  const loading = ref(false)
  const refreshing = ref(false)
  const error = ref('')

  /** 包装一次加载：负责开关 loading/error，并把异常交回给调用方处理 */
  async function run<T>(task: () => Promise<T>, hasData: () => boolean): Promise<T | undefined> {
    const firstTime = !hasData()
    if (firstTime) {
      loading.value = true
      error.value = ''
    } else {
      refreshing.value = true
    }
    try {
      return await task()
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err)
      console.error('[prediction] 加载失败：', msg)
      // 已有数据时刷新失败：不动 error，避免整页变错误态，交给页面弹轻提示
      if (firstTime) error.value = msg
      throw err
    } finally {
      loading.value = false
      refreshing.value = false
    }
  }

  return { loading, refreshing, error, run }
}

// ══════════════════════════════════════════════════════════════════
// 页面2：权威解析
// ══════════════════════════════════════════════════════════════════
export const useAnalysisStore = defineStore('predictionAnalysis', () => {
  const analyses = ref<ExpertAnalysis[]>([])
  /** 按解析 id 缓存评论：点开哪条才加载哪条，首屏不用背全部评论 */
  const commentsMap = ref<Record<number, ExpertComment[]>>({})
  const { loading, refreshing, error, run } = useLoadState()

  async function loadAnalyses(): Promise<void> {
    await run(async () => {
      analyses.value = await fetchExpertAnalysis()
    }, () => analyses.value.length > 0)
  }

  async function loadComments(analysisId: number): Promise<void> {
    await run(async () => {
      const list = await fetchAnalysisComments(analysisId)
      // 用新对象替换，保证 Vue 能侦测到这个 key 的变化
      commentsMap.value = { ...commentsMap.value, [analysisId]: list }
    }, () => (commentsMap.value[analysisId] ?? []).length > 0)
  }

  /**
   * 发表评论。
   * 成功后把返回的这条追加进本地列表，页面立刻看到自己的发言 ——
   * 不必为此重新拉一次列表（省一次往返，也不用担心列表顺序变来变去）。
   */
  async function addComment(analysisId: number, content: string): Promise<void> {
    const created = await postExpertComment(analysisId, content)
    const old = commentsMap.value[analysisId] ?? []
    commentsMap.value = { ...commentsMap.value, [analysisId]: [...old, created] }
    // 卡片上的「N 条评论」跟着 +1，避免明明发出去了数字却没动
    const target = analyses.value.find((a) => a.id === analysisId)
    if (target) target.commentCount += 1
  }

  const commentsOf = computed(
    () => (analysisId: number) => commentsMap.value[analysisId] ?? []
  )

  return { analyses, commentsMap, loading, refreshing, error, loadAnalyses, loadComments, addComment, commentsOf }
})

// ══════════════════════════════════════════════════════════════════
// 页面4：AI 预测
// ══════════════════════════════════════════════════════════════════
export const usePredictionStore = defineStore('predictionTeams', () => {
  const teams = ref<TeamPrediction[]>([])
  const weights = ref<DimScore[]>([])
  const { loading, refreshing, error, run } = useLoadState()

  async function loadPredictions(): Promise<void> {
    await run(async () => {
      teams.value = await fetchPredictions()
    }, () => teams.value.length > 0)
  }

  async function loadAlgorithm(): Promise<void> {
    await run(async () => {
      weights.value = await fetchAlgorithm()
    }, () => weights.value.length > 0)
  }

  /** 权重合计（展示用，正常应正好 100；不等于 100 说明后端权重配错了，页面会显示出来） */
  const weightTotal = computed(() => weights.value.reduce((sum, w) => sum + w.score, 0))

  return { teams, weights, loading, refreshing, error, loadPredictions, loadAlgorithm, weightTotal }
})

// ══════════════════════════════════════════════════════════════════
// 页面5：历史回顾
// ══════════════════════════════════════════════════════════════════
export const useHistoryStore = defineStore('predictionHistory', () => {
  const seasons = ref<SeasonHistory[]>([])
  const titles = ref<TitleCount[]>([])
  const { loading, refreshing, error, run } = useLoadState()

  async function loadSeasons(): Promise<void> {
    await run(async () => {
      seasons.value = await fetchSeasonHistory()
    }, () => seasons.value.length > 0)
  }

  async function loadTitleCounts(): Promise<void> {
    await run(async () => {
      titles.value = await fetchTitleCounts()
    }, () => titles.value.length > 0)
  }

  /** 届数范围，如「第 9 - 第 33 届」：给表格一个上下文，而不是一堆孤零零的行 */
  const editionRange = computed(() => {
    if (seasons.value.length === 0) return ''
    // 接口按届数倒序，所以末尾才是最早的那一届
    const newest = seasons.value[0].edition
    const oldest = seasons.value[seasons.value.length - 1].edition
    return oldest === newest ? `第 ${newest} 届` : `第 ${oldest} - 第 ${newest} 届`
  })

  return { seasons, titles, loading, refreshing, error, loadSeasons, loadTitleCounts, editionRange }
})
