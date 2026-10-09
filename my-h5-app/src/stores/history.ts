import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchSeasonHistory, fetchTitleCounts } from '@/api/history'
import type { SeasonHistory, TitleCount } from '@/types/api'
import { useLoadState } from './useLoadState'

/**
 * 页面5 历史回顾状态。
 * 三态、加载机见 useLoadState.ts。
 */
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
