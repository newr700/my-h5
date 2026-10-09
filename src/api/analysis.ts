import { get, post } from './request'
import { asArray, asNumber, asString, expectCount, isPlainObject } from '@/utils/validate'
import type { ExpertAnalysis, ExpertComment } from '@/types/api'

/**
 * 页面2 权威解析：专家观点列表 + 评论（含发表）。
 * 校验规矩见 predictionShared.ts 顶部说明。
 */

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
