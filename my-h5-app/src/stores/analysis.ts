import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchAnalysisComments, fetchExpertAnalysis, postExpertComment } from '@/api/analysis'
import type { ExpertAnalysis, ExpertComment } from '@/types/api'
import { useLoadState } from './useLoadState'

/**
 * 页面2 权威解析状态。
 * 三态、加载机见 useLoadState.ts。
 */
export const useAnalysisStore = defineStore('predictionAnalysis', () => {
  const analyses = ref<ExpertAnalysis[]>([])
  /** 按解析 id 存放评论：进入页面时一次性拉全，展开只是本地切换 */
  const commentsMap = ref<Record<number, ExpertComment[]>>({})
  /** 评论是否已批量拉取完成（用于区分「真的没人评论」和「还没拉回来」） */
  const commentsReady = ref(false)
  /** 单条解析的评论加载失败原因，key 是解析 id，值为空串表示正常 */
  const commentsError = ref<Record<number, string>>({})
  const { loading, refreshing, error, run } = useLoadState()

  async function loadAnalyses(): Promise<void> {
    await run(async () => {
      analyses.value = await fetchExpertAnalysis()
    }, () => analyses.value.length > 0)
  }

  /**
   * 一次性把所有解析的评论拉全 —— 取代原来的「点开哪条才拉哪条」。
   *
   * ── 为什么不再懒加载 ──────────────────────────────────────
   * 懒加载下，评论区展开的瞬间数据还没到，先显示空态、数据到了又突然填满，
   * 观感上就是「闪一下」。解析条目只有个位数，并发拉的成本远低于这个体验代价。
   */
  async function loadAllComments(analysisIds: number[]): Promise<void> {
    commentsReady.value = false
    commentsError.value = {}
    if (analysisIds.length === 0) {
      commentsReady.value = true
      return
    }
    const results = await Promise.allSettled(analysisIds.map((id) => fetchAnalysisComments(id)))
    const next = { ...commentsMap.value }
    const errs: Record<number, string> = {}
    results.forEach((r, i) => {
      const id = analysisIds[i]
      if (r.status === 'fulfilled') {
        next[id] = r.value
      } else {
        errs[id] = r.reason instanceof Error ? r.reason.message : String(r.reason)
      }
    })
    // 用新对象替换，保证 Vue 能侦测到这些 key 的变化
    commentsMap.value = next
    commentsError.value = errs
    commentsReady.value = true
  }

  /**
   * 只重拉某一条的评论（用于失败重试）。
   * 这里刻意不走 useLoadState().run —— run 在「该条还没数据」时会把 loading 置为 true，
   * 而页面的 loading 是全屏骨架屏，重试一条评论却让整页变骨架是不对的。
   */
  async function loadComments(analysisId: number): Promise<void> {
    try {
      const list = await fetchAnalysisComments(analysisId)
      commentsMap.value = { ...commentsMap.value, [analysisId]: list }
      commentsError.value = { ...commentsError.value, [analysisId]: '' }
    } catch (err) {
      commentsError.value = {
        ...commentsError.value,
        [analysisId]: err instanceof Error ? err.message : String(err)
      }
      throw err
    }
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

  return {
    analyses,
    commentsMap,
    commentsReady,
    commentsError,
    loading,
    refreshing,
    error,
    loadAnalyses,
    loadAllComments,
    loadComments,
    addComment,
    commentsOf
  }
})
