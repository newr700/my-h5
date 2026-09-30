import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchStandings } from '@/api/standings'
import type { TeamStanding } from '@/types/api'

/**
 * 后端还没起，先用假数据顶上，页面可以正常开发。
 * 联调后删掉这个常量和 loadStandings 里 catch 的兜底即可。
 */
const mockStandings: TeamStanding[] = [
  { rank: 1, teamName: '球队1', played: 9, win: 9, draw: 0, lose: 0, goalsFor: 20, goalsAgainst: 2, points: 27 },
  { rank: 2, teamName: '球队2', played: 9, win: 7, draw: 1, lose: 1, goalsFor: 18, goalsAgainst: 6, points: 22 },
  { rank: 3, teamName: '球队3', played: 9, win: 6, draw: 2, lose: 1, goalsFor: 15, goalsAgainst: 7, points: 20 },
  { rank: 4, teamName: '球队4', played: 9, win: 6, draw: 1, lose: 2, goalsFor: 14, goalsAgainst: 8, points: 19 },
  { rank: 5, teamName: '球队5', played: 9, win: 5, draw: 3, lose: 1, goalsFor: 13, goalsAgainst: 7, points: 18 },
  { rank: 6, teamName: '球队6', played: 9, win: 5, draw: 2, lose: 2, goalsFor: 12, goalsAgainst: 9, points: 17 },
  { rank: 7, teamName: '球队7', played: 9, win: 5, draw: 1, lose: 3, goalsFor: 12, goalsAgainst: 10, points: 16 },
  { rank: 8, teamName: '球队8', played: 9, win: 4, draw: 3, lose: 2, goalsFor: 11, goalsAgainst: 9, points: 15 },
  { rank: 9, teamName: '球队9', played: 9, win: 4, draw: 2, lose: 3, goalsFor: 11, goalsAgainst: 11, points: 14 },
  { rank: 10, teamName: '球队10', played: 9, win: 4, draw: 1, lose: 4, goalsFor: 10, goalsAgainst: 12, points: 13 },
  { rank: 11, teamName: '球队11', played: 9, win: 3, draw: 3, lose: 3, goalsFor: 9, goalsAgainst: 10, points: 12 },
  { rank: 12, teamName: '球队12', played: 9, win: 3, draw: 2, lose: 4, goalsFor: 9, goalsAgainst: 12, points: 11 },
  { rank: 13, teamName: '球队13', played: 9, win: 3, draw: 1, lose: 5, goalsFor: 8, goalsAgainst: 13, points: 10 },
  { rank: 14, teamName: '球队14', played: 9, win: 2, draw: 4, lose: 3, goalsFor: 8, goalsAgainst: 11, points: 10 },
  { rank: 15, teamName: '球队15', played: 9, win: 2, draw: 3, lose: 4, goalsFor: 7, goalsAgainst: 12, points: 9 },
  { rank: 16, teamName: '球队16', played: 9, win: 2, draw: 2, lose: 5, goalsFor: 7, goalsAgainst: 14, points: 8 },
  { rank: 17, teamName: '球队17', played: 9, win: 2, draw: 1, lose: 6, goalsFor: 6, goalsAgainst: 15, points: 7 },
  { rank: 18, teamName: '球队18', played: 9, win: 1, draw: 3, lose: 5, goalsFor: 5, goalsAgainst: 13, points: 6 },
  { rank: 19, teamName: '球队19', played: 9, win: 1, draw: 1, lose: 7, goalsFor: 4, goalsAgainst: 17, points: 4 },
  { rank: 20, teamName: '球队20', played: 9, win: 0, draw: 2, lose: 7, goalsFor: 3, goalsAgainst: 19, points: 2 }
]

/** 积分榜模块状态（样例模块，供参考模仿） */
export const useStandingsStore = defineStore('standings', () => {
  const standings = ref<TeamStanding[]>([])
  const loading = ref(false)
  /** 失败原因 —— 契约校验的错误信息也会出现在这里 */
  const error = ref('')

  async function loadStandings() {
    loading.value = true
    error.value = ''
    try {
      standings.value = await fetchStandings()
    } catch (err) {
      // 【这里有个坑，你们一定要知道】
      // 下面那句「用假数据兜底」会把真实错误彻底遮住：
      // 页面看着一切正常，其实数据全是假的，真到了线上没人知道接口早就坏了。
      // 所以兜底之前必须先把错误存进 error 并打到控制台 —— 看不见的错误才是最可怕的。
      error.value = err instanceof Error ? err.message : String(err)
      console.error('[standings] 加载失败：', error.value)
      standings.value = mockStandings
    } finally {
      loading.value = false
    }
  }

  // 按草图的分区规则切片：1-6 欧冠区、7-17 欧联区、18-20 降级区
  const championsLeagueZone = computed(() => standings.value.slice(0, 6))
  const europaLeagueZone = computed(() => standings.value.slice(6, 17))
  const relegationZone = computed(() => standings.value.slice(17, 20))

  return {
    standings,
    loading,
    error,
    loadStandings,
    championsLeagueZone,
    europaLeagueZone,
    relegationZone
  }
})
