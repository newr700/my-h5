import { get, post } from './request'
import { asArray, asNumber, asString, expectCount, isPlainObject } from '@/utils/validate'
import type {
  DimScore,
  ExpertAnalysis,
  ExpertComment,
  SeasonHistory,
  StarPlayer,
  TeamBrief,
  TeamPrediction,
  TitleCount
} from '@/types/api'

/**
 * 英超预测模块的接口层（页面2 权威解析 / 页面4 AI预测 / 页面5 历史回顾）
 *
 * ── 沿用 standings.ts 的规矩 ─────────────────────────────────
 * 一律 `get<unknown>` + 逐字段 parse 校验，而不是直接 `get<Xxx[]>`。
 * 后者的泛型只是一句「我猜它长这样」，编译后一个字都不剩 ——
 * 后端真少返回一个字段时，页面只会显示 undefined 而不报错，最难查的那类 bug。
 *
 * ── 数量校验的力度怎么定 ────────────────────────────────────
 * 写死条数（如 expectCount(list, path, 20)）适用于「数量本身就是业务约定」的数据；
 * 由运营在后台增删的内容（专家观点、点评、赛季）用【范围】，
 * 免得运营多录一位专家，整个页面就白屏 —— 那属于把后端的数据决策写死在前端里。
 */

/** 雷达图的顶点数：形状写死才有意义（少一维图就画不圆），所以这里是严格数字 */
const DIM_COUNT = 5

/** 把后端返回的一支球队资料，逐字段校验成 TeamBrief */
function parseTeamBrief(raw: unknown, path: string): TeamBrief {
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

function parseDim(raw: unknown, path: string): DimScore {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] ${path} 应该是一个对象，实际收到 ${typeof raw}`)
  }
  return {
    key: asString(raw.key, `${path}.key`),
    label: asString(raw.label, `${path}.label`),
    score: asNumber(raw.score, `${path}.score`)
  }
}

function parseStarPlayer(raw: unknown, path: string): StarPlayer {
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

// ══════════════════════════════════════════════════════════════════
// 页面2：权威解析
// ══════════════════════════════════════════════════════════════════

function parseAnalysis(raw: unknown, index: number): ExpertAnalysis {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] analysis[${index}] 应该是一个对象，实际收到 ${typeof raw}`)
  }
  const at = (field: string) => `analysis[${index}].${field}`
  return {
    // id 是「点开评论」和「发评论」都要用的主键，绝不能错
    id: asNumber(raw.id, at('id')),
    nameEn: asString(raw.nameEn, at('nameEn')),
    nameCn: asString(raw.nameCn, at('nameCn')),
    title: asString(raw.title, at('title'), ''),
    teamName: asString(raw.teamName, at('teamName')),
    teamNameEn: asString(raw.teamNameEn, at('teamNameEn')),
    shortName: asString(raw.shortName, at('shortName'), 'TBD'),
    colorPrimary: asString(raw.colorPrimary, at('colorPrimary'), '#909399'),
    colorSecondary: asString(raw.colorSecondary, at('colorSecondary'), '#C0C4CC'),
    logoUrl: asString(raw.logoUrl, at('logoUrl'), ''),
    reason: asString(raw.reason, at('reason')),
    avatarUrl: asString(raw.avatarUrl, at('avatarUrl'), ''),
    commentCount: asNumber(raw.commentCount, at('commentCount'), 0)
  }
}

/** 专家观点列表（含各自评论条数） */
export async function fetchExpertAnalysis(): Promise<ExpertAnalysis[]> {
  const raw = await get<unknown>('/prediction/analysis')
  const list = asArray(raw, 'analysis')
  // 范围而非写死：专家是后台可增删的内容
  expectCount(list, 'analysis', { min: 1, max: 50 })
  return list.map(parseAnalysis)
}

function parseComment(raw: unknown, index: number): ExpertComment {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] comments[${index}] 应该是一个对象，实际收到 ${typeof raw}`)
  }
  const at = (field: string) => `comments[${index}].${field}`
  return {
    id: asNumber(raw.id, at('id')),
    analysisId: asNumber(raw.analysisId, at('analysisId')),
    // 演示评论没有真实用户：userId 可以是 null，所以这里允许 null 通过
    userId: raw.userId === null ? null : asNumber(raw.userId, at('userId')),
    nickname: asString(raw.nickname, at('nickname')),
    userLevel: asNumber(raw.userLevel, at('userLevel')),
    content: asString(raw.content, at('content')),
    createdAt: asString(raw.createdAt, at('createdAt'))
  }
}

/** 某条解析下的评论（按时间正序） */
export async function fetchAnalysisComments(analysisId: number): Promise<ExpertComment[]> {
  const raw = await get<unknown>(`/prediction/analysis/${analysisId}/comments`)
  const list = asArray(raw, 'comments')
  expectCount(list, 'comments', { max: 500 })
  return list.map(parseComment)
}

/**
 * 发表评论。
 *
 * 后端会在这一刻做真正的校验：未登录 401、等级不足 6002。
 * 前端在此之前把输入框藏起来，纯粹是怕用户打完字才被告知没权限 —— 那是体验，不是安全。
 */
export async function postExpertComment(
  analysisId: number,
  content: string
): Promise<ExpertComment> {
  const raw = await post<unknown>(`/prediction/analysis/${analysisId}/comment`, { content })
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] 发表评论的返回应该是一个对象，实际收到 ${typeof raw}`)
  }
  // 单条数据也走同一套 parse，省不得 —— 「成功」是最容易放松校验的时刻
  return parseComment(raw, 0)
}

// ══════════════════════════════════════════════════════════════════
// 页面4：AI 预测
// ══════════════════════════════════════════════════════════════════

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

// ══════════════════════════════════════════════════════════════════
// 页面5：历史回顾
// ══════════════════════════════════════════════════════════════════

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
