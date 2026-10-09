import { get } from './request'
import { asArray, asNumber, asString, expectCount, isPlainObject } from '@/utils/validate'
import type { SeasonHistory, TitleCount } from '@/types/api'
import { parseTeamBrief } from './predictionShared'

/**
 * 页面5 历史回顾：历届战绩 + 全时期夺冠次数。
 * 校验规矩见 predictionShared.ts 顶部说明。
 */

function parseSeason(raw: unknown, index: number): SeasonHistory {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] history[${index}] 应该是一个对象，实际收到 ${typeof raw}`)
  }
  const at = (field: string) => `history[${index}].${field}`
  return {
    edition: asNumber(raw.edition, at('edition')),
    seasonYear: asNumber(raw.seasonYear, at('seasonYear')),
    seasonLabel: asString(raw.seasonLabel, at('seasonLabel')),
    champion: parseTeamBrief(raw.champion, at('champion')),
    runnerUp: parseTeamBrief(raw.runnerUp, at('runnerUp')),
    third: parseTeamBrief(raw.third, at('third')),
    fourth: parseTeamBrief(raw.fourth, at('fourth'))
  }
}

/** 历届战绩（按届数倒序） */
export async function fetchSeasonHistory(): Promise<SeasonHistory[]> {
  const raw = await get<unknown>('/prediction/history')
  const list = asArray(raw, 'history')
  // 赛季只会越加越多，给一个远大于现状的上限即可（防后端忘了分页）
  expectCount(list, 'history', { min: 1, max: 200 })
  return list.map(parseSeason)
}

function parseTitleCount(raw: unknown, index: number): TitleCount {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] titles[${index}] 应该是一个对象，实际收到 ${typeof raw}`)
  }
  return {
    team: parseTeamBrief(raw.team, `titles[${index}].team`),
    count: asNumber(raw.count, `titles[${index}].count`)
  }
}

/** 英超历届夺冠次数（全时期，后端现算 GROUP BY，追加赛季自动更新） */
export async function fetchTitleCounts(): Promise<TitleCount[]> {
  const raw = await get<unknown>('/prediction/history/titles')
  const list = asArray(raw, 'titles')
  // 并列名次可能让实际条数略多于 5，所以上限放宽而不是写死 5
  expectCount(list, 'titles', { min: 1, max: 10 })
  return list.map(parseTitleCount)
}
