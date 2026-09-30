import { get } from './request'
import { asArray, asNumber, asString, expectCount, isPlainObject } from '@/utils/validate'
import type { TeamStanding } from '@/types/api'

/**
 * 积分榜模块的所有接口（样例模块，供参考模仿）
 *
 * ② 接口层：页面不许直接写 axios，所有请求都收敛成这样的函数。
 *
 * ── 面试技能点（详见 docs/面试技能树.md T0/T4）────────────
 * 本文件：unknown 渐进收窄 / 泛型传参 / RESTful 路径设计
 * 考点速记：为什么用 get<unknown> 而不是 get<TeamStanding[]>？
 * （答：后者是口头信任，前者配合 parse 才是"先检查再使用"）
 *
 * ── 关键：get 的泛型只是一句「我猜它长这样」 ──────────────────
 * 以前写的是 get<TeamStanding[]>，等于无条件信任后端。
 * 正确写法：先当它是 unknown（我不知道它长什么样），
 * 再用 parseXxx 逐字段对照检查，检查通过才敢当成 TeamStanding[] 用。
 */

/** 联赛固定队伍数；如果以后规则变了，改成 { min: 1, max: 30 } 更稳妥 */
const LEAGUE_TEAM_COUNT = 20

/** 把后端返回的一支球队数据，逐字段校验成 TeamStanding */
function parseStanding(raw: unknown, index: number): TeamStanding {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] standings[${index}] 应该是一个对象，实际收到 ${typeof raw}`)
  }

  // path 里带上数组下标，报错时能直接定位到第几条数据哪个字段
  const at = (field: string) => `standings[${index}].${field}`

  return {
    rank: asNumber(raw.rank, at('rank')),
    teamName: asString(raw.teamName, at('teamName')),
    played: asNumber(raw.played, at('played')),
    win: asNumber(raw.win, at('win')),
    draw: asNumber(raw.draw, at('draw')),
    lose: asNumber(raw.lose, at('lose')),
    goalsFor: asNumber(raw.goalsFor, at('goalsFor')),
    goalsAgainst: asNumber(raw.goalsAgainst, at('goalsAgainst')),
    // 积分错了整张榜都是错的，这种核心数据必须用严格模式（不给兜底值）
    points: asNumber(raw.points, at('points')),
    // 队徽是展示类次要字段：后端暂时没返回，所以用宽松模式。
    // 缺失时静默兜底成 ''，页面显示排名圆圈；将来后端补了 logoUrl 字段，这里一行不用改
    logoUrl: asString(raw.logoUrl, at('logoUrl'), '')
  }
}

export async function fetchStandings(): Promise<TeamStanding[]> {
  const raw = await get<unknown>('/standings')

  const list = asArray(raw, 'standings')
  expectCount(list, 'standings', LEAGUE_TEAM_COUNT)

  return list.map(parseStanding)
}

/**
 * ── 想练习的话，对照写一下 user 和 order ─────────────────────
 *
 * // user.ts：对象类型（不是数组），记得先判断是否对象
 * export async function fetchUserProfile(): Promise<UserProfile> {
 *   const raw = await get<unknown>('/user/profile')
 *   if (!isPlainObject(raw)) throw new Error('[契约校验] user/profile 应该是对象')
 *   return {
 *     id: asNumber(raw.id, 'profile.id'),
 *     nickname: asString(raw.nickname, 'profile.nickname'),
 *     // 头像没了不致命，给个默认头像兜底，开发环境会告警
 *     avatar: asString(raw.avatar, 'profile.avatar', '/default-avatar.png')
 *   }
 * }
 *
 * // order.ts：带 enum 字段的列表，后端多返回一种状态也拦得住
 * export async function fetchOrderList(): Promise<OrderItem[]> {
 *   const raw = await get<unknown>('/order/list')
 *   const list = asArray(raw, 'orderList')
 *   expectCount(list, 'orderList', { max: 100 })   // 至多 100 条，防止后端忘了分页
 *   return list.map((item, i) => {
 *     if (!isPlainObject(item)) throw new Error(`[契约校验] orderList[${i}] 应该是对象`)
 *     return {
 *       id: asNumber(item.id, `orderList[${i}].id`),
 *       title: asString(item.title, `orderList[${i}].title`),
 *       amount: asNumber(item.amount, `orderList[${i}].amount`),
 *       status: asEnum(item.status, ['pending', 'paid', 'closed'], `orderList[${i}].status`)
 *     }
 *   })
 * }
 */
