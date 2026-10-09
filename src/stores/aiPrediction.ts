import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchAlgorithm, fetchPredictions } from '@/api/aiPrediction'
import type { DimScore, TeamPrediction } from '@/types/api'
import { useLoadState } from './useLoadState'

/**
 * 页面4 AI 预测状态。
 * 三态、加载机见 useLoadState.ts。
 */
export const useAiPredictionStore = defineStore('predictionTeams', () => {
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

  return { teams, weights, loading, refreshing, error, loadPredictions, loadAlgorithm }
})
