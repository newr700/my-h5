import { get } from './request'
import { asArray, asNumber, asString, expectCount, isPlainObject } from '@/utils/validate'
import type { DimScore, TeamPrediction } from '@/types/api'
import { DIM_COUNT, parseDim, parseStarPlayer } from './predictionShared'

/**
 * 页面4 AI 预测：球队夺冠概率排名 + 评估维度说明。
 * 校验规矩见 predictionShared.ts 顶部说明。
 */

function parsePrediction(raw: unknown, index: number): TeamPrediction {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] teams[${index}] 应该是一个对象，实际收到 ${typeof raw}`)
  }
  const at = (field: string) => `teams[${index}].${field}`

  // 概率和排名是这一页的核心：错了整页结论都是错的，用严格模式
  const rawDims = asArray(raw.dims, at('dims'))
  expectCount(rawDims, at('dims'), DIM_COUNT)

  return {
    rank: asNumber(raw.rank, at('rank')),
    teamName: asString(raw.teamName, at('teamName')),
    teamNameEn: asString(raw.teamNameEn, at('teamNameEn')),
    shortName: asString(raw.shortName, at('shortName'), 'TBD'),
    colorPrimary: asString(raw.colorPrimary, at('colorPrimary'), '#909399'),
    colorSecondary: asString(raw.colorSecondary, at('colorSecondary'), '#C0C4CC'),
    logoUrl: asString(raw.logoUrl, at('logoUrl'), ''),
    winProbability: asNumber(raw.winProbability, at('winProbability')),
    dims: rawDims.map((d, i) => parseDim(d, `teams[${index}].dims[${i}]`)),
    // 分析文字是文档给定的，缺失就当空串（该队卡片不显示分析块）
    analysis: asString(raw.analysis, at('analysis'), ''),
    // 明星球员是可选内容：没配就是空数组，页面显示「暂无」而不是报错
    starPlayers: (raw.starPlayers === undefined || raw.starPlayers === null
      ? []
      : asArray(raw.starPlayers, at('starPlayers'))
    ).map((s, i) => parseStarPlayer(s, `teams[${index}].starPlayers[${i}]`))
  }
}

/** 球队夺冠概率排名（后端已排好序，前端不再排序） */
export async function fetchPredictions(): Promise<TeamPrediction[]> {
  const raw = await get<unknown>('/prediction/teams')
  const list = asArray(raw, 'teams')
  expectCount(list, 'teams', { min: 1, max: 30 })
  return list.map(parsePrediction)
}

/** AI 模型评估维度说明 —— 页面底部「数据说明」区展示维度名称与顺序 */
export async function fetchAlgorithm(): Promise<DimScore[]> {
  const raw = await get<unknown>('/prediction/algorithm')
  const list = asArray(raw, 'algorithm')
  expectCount(list, 'algorithm', DIM_COUNT)
  return list.map((d, i) => parseDim(d, `algorithm[${i}]`))
}
