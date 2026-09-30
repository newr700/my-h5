import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchStandings } from '@/api/standings'
import type { TeamStanding } from '@/types/api'

/**
 * 积分榜模块状态（样例模块，供参考模仿）
 *
 * ── 清单落地：列表三态 ──────────────────────────────────
 * 一个「能上线」的列表页必须处理三种状态：
 *   1. 加载中（loading）    —— 首次进入，页面还没数据 → 骨架屏
 *   2. 加载失败（error）    —— 没数据可用时 → 全屏错误 + 重试按钮
 *   3. 有数据（standings）  —— 正常渲染
 * 另有第四种：刷新中（refreshing）—— 已有数据时重新拉取，不打断浏览
 *
 * ── 关于「假数据兜底」 ─────────────────────────────────
 * 之前 catch 里塞了 mockStandings 让页面永远有东西可看。
 * 现在后端已跑通，按清单「错误必须可感知」的要求删掉了：
 * 接口坏了就堂堂正正显示错误态，而不是拿假数据骗人。
 * （想恢复临时开发用，git 历史里能找回，但别带到生产。）
 */
export const useStandingsStore = defineStore('standings', () => {
  const standings = ref<TeamStanding[]>([])
  /** 首次加载中：页面一条数据都还没有 */
  const loading = ref(false)
  /** 下拉刷新中：已有数据，正在更新 */
  const refreshing = ref(false)
  /** 仅在「没有任何数据可用」时展示（首次加载失败的完整原因） */
  const error = ref('')

  /**
   * 加载积分榜。
   * 约定：无论成功失败都会把状态写进上面的 ref，然后把异常继续抛给调用方，
   * 由页面层决定怎么呈现（首次失败走全屏错误态；刷新失败走轻提示）。
   */
  async function loadStandings(): Promise<void> {
    const firstTime = standings.value.length === 0
    if (firstTime) {
      loading.value = true
      error.value = ''
    } else {
      refreshing.value = true
    }
    try {
      standings.value = await fetchStandings()
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err)
      console.error('[standings] 加载失败：', msg)
      if (firstTime) {
        // 首次就失败：页面没有任何数据，错误必须占据整个屏幕让人看见
        error.value = msg
      }
      // 已有数据时刷新失败：不动 error，交给页面弹轻提示，不打断用户浏览
      throw err
    } finally {
      loading.value = false
      refreshing.value = false
    }
  }

  // 按草图的分区规则切片：1-6 欧冠区、7-17 欧联区、18-20 降级区
  const championsLeagueZone = computed(() => standings.value.slice(0, 6))
  const europaLeagueZone = computed(() => standings.value.slice(6, 17))
  const relegationZone = computed(() => standings.value.slice(17, 20))

  /** 副标题：取第一条的已赛轮次，如「第 9 轮战罢」 */
  const roundSummary = computed(() =>
    standings.value.length > 0 ? `第 ${standings.value[0].played} 轮战罢` : ''
  )

  return {
    standings,
    loading,
    refreshing,
    error,
    loadStandings,
    championsLeagueZone,
    europaLeagueZone,
    relegationZone,
    roundSummary
  }
})
