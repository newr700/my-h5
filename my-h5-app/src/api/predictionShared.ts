import { asNumber, asString, isPlainObject } from '@/utils/validate'
import type { TeamBrief, DimScore, StarPlayer } from '@/types/api'

/**
 * 英超预测模块三个页面的【共享解析工具】。
 *
 * ── 规矩（沿用 standings.ts）──────────────────────────────
 * 一律 get<unknown> + 逐字段 parse 校验，而非 get<Xxx[]>，
 * 后端真少返回字段时页面会直接报错而不是显示 undefined。
 *
 * ── 这里为什么单独成文件 ─────────────────────────────────
 * TeamBrief / DimScore / StarPlayer 是跨页面复用的 VO 形状
 * （历史回顾的冠亚季军用 TeamBrief，AI 预测的维度/球员用 DimScore/StarPlayer），
 * 把它们的 parse 收在一处，单一事实来源、三页共用。
 */

/** 雷达图的顶点数：形状写死才有意义（少一维图就画不圆），所以这里是严格数字 */
export const DIM_COUNT = 5

/** 把后端返回的一支球队资料，逐字段校验成 TeamBrief */
export function parseTeamBrief(raw: unknown, path: string): TeamBrief {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] ${path} 应该是一个对象，实际收到 ${typeof raw}`)
  }
  return {
    teamName: asString(raw.teamName, `${path}.teamName`),
    shortName: asString(raw.shortName, `${path}.shortName`),
    // 颜色是展示类字段：后端没给就退化成灰色，总比整页报错强
    colorPrimary: asString(raw.colorPrimary, `${path}.colorPrimary`, '#909399'),
    colorSecondary: asString(raw.colorSecondary, `${path}.colorSecondary`, '#C0C4CC'),
    logoUrl: asString(raw.logoUrl, `${path}.logoUrl`, '')
  }
}

/** 把后端返回的一个评估维度，逐字段校验成 DimScore */
export function parseDim(raw: unknown, path: string): DimScore {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] ${path} 应该是一个对象，实际收到 ${typeof raw}`)
  }
  return {
    key: asString(raw.key, `${path}.key`),
    label: asString(raw.label, `${path}.label`),
    score: asNumber(raw.score, `${path}.score`)
  }
}

/** 把后端返回的一名球星，逐字段校验成 StarPlayer */
export function parseStarPlayer(raw: unknown, path: string): StarPlayer {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] ${path} 应该是一个对象，实际收到 ${typeof raw}`)
  }
  return {
    playerName: asString(raw.playerName, `${path}.playerName`),
    position: asString(raw.position, `${path}.position`),
    jerseyNumber: asNumber(raw.jerseyNumber, `${path}.jerseyNumber`),
    photoUrl: asString(raw.photoUrl, `${path}.photoUrl`, '')
  }
}
