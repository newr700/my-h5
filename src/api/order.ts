import { get, post } from './request'
import { asArray, asEnum, asNumber, asString, expectCount, isPlainObject } from '@/utils/validate'
import type { MatchInfo, OrderItem, PageResult } from '@/types/api'

/**
 * 订单/比赛模块接口（对应 Java 端 OrderController / MatchController）。
 *
 * 写操作闭环的前端一半：
 *   fetchMatches（选商品）→ createOrder（下单）→ fetchOrders（看结果）
 *   → payOrder / cancelOrder（推状态机）
 * 金额永远以响应里后端算好的为准，前端不在本地算总价 —— 展示都未必需要算。
 */

/**
 * ── V2 第二步：幂等（防重复下单）──────────────────────────
 * pendingRequestId 是「幂等号」的客户端一端：
 *   · 发起一笔新下单时，若还没有号，就生成一个（crypto.randomUUID）；
 *   · 下单【成功】→ 清空，下一笔用新号；
 *   · 下单【失败】→ 保留，重试 / 用户再点时复用同一号。
 * 后端按 (userId, requestId) 查到原订单就直接返回（不重复扣票），
 * 唯一索引 (user_id, request_id) 在并发下兜底。详见后端 OrderService 注释与 V4__idempotency.sql。
 *
 * 为什么放在 api 层而不是页面层：requestId 是「一次下单意图」的凭证，
 * 和请求封装同层，页面完全不用关心 —— 这也符合「请求细节收敛到唯一出口」的原则。
 */
let pendingRequestId: string | null = null

/** 订单状态的全部合法值 —— 后端新增状态（如 refunded）会在这里被立刻拦下报错 */
const ORDER_STATUSES = ['pending', 'paid', 'closed'] as const

function parseMatch(raw: unknown, index: number): MatchInfo {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] matches[${index}] 应该是一个对象`)
  }
  const at = (field: string) => `matches[${index}].${field}`
  return {
    id: asNumber(raw.id, at('id')),
    matchTitle: asString(raw.matchTitle, at('matchTitle')),
    matchTime: asString(raw.matchTime, at('matchTime')),
    // 金额是核心数据：严格模式，错了宁可页面报错也不能让用户下错单
    unitPrice: asNumber(raw.unitPrice, at('unitPrice')),
    // V2：库存字段同样用严格模式 —— 票数是「能不能买」的依据，
    // 若因为字段缺失被 quietly 当成 undefined，前端算出来的「剩余」会是 NaN，
    // 按钮状态全乱。宁可在这里炸清楚，也不要带着脏数据往后走
    totalStock: asNumber(raw.totalStock, at('totalStock')),
    stock: asNumber(raw.stock, at('stock'))
  }
}

function parseOrder(raw: unknown, index: number): OrderItem {
  if (!isPlainObject(raw)) {
    throw new Error(`[契约校验] orders[${index}] 应该是一个对象`)
  }
  const at = (field: string) => `orders[${index}].${field}`
  return {
    id: asNumber(raw.id, at('id')),
    orderNo: asString(raw.orderNo, at('orderNo')),
    matchTitle: asString(raw.matchTitle, at('matchTitle')),
    matchTime: asString(raw.matchTime, at('matchTime')),
    quantity: asNumber(raw.quantity, at('quantity')),
    unitPrice: asNumber(raw.unitPrice, at('unitPrice')),
    totalAmount: asNumber(raw.totalAmount, at('totalAmount')),
    status: asEnum(raw.status, ORDER_STATUSES, at('status')),
    createdAt: asString(raw.createdAt, at('createdAt'))
  }
}

/** 在售比赛列表（公开接口，不需要登录） */
export async function fetchMatches(): Promise<MatchInfo[]> {
  const raw = await get<unknown>('/matches')
  const list = asArray(raw, 'matches')
  expectCount(list, 'matches', { min: 1 })
  return list.map(parseMatch)
}

/** 我的订单（分页，工程手册 4.3：pageNum 从 1 开始） */
export async function fetchOrders(pageNum = 1, pageSize = 50): Promise<PageResult<OrderItem>> {
  const raw = await get<unknown>('/order/list', { params: { pageNum, pageSize } })
  if (!isPlainObject(raw)) {
    throw new Error('[契约校验] order/list 应该是一个分页对象')
  }
  const list = asArray(raw.list, 'orderList.list')
  return {
    list: list.map(parseOrder),
    total: asNumber(raw.total, 'orderList.total'),
    pageNum: asNumber(raw.pageNum, 'orderList.pageNum'),
    pageSize: asNumber(raw.pageSize, 'orderList.pageSize')
  }
}

/**
 * 创建订单 —— 注意请求体里【没有】金额。
 * 这是契约层面的安全设计：金额由后端拿 matchId 查库重算（PRD 5.2），
 * 前端想传都传不进去（DTO 白名单会丢弃多余字段）。
 *
 * V2 第二步：每次调用都带上 pendingRequestId（幂等号）。
 * 成功 → 清空（下一笔换新号）；失败 → 保留（重试复用同一号 → 后端幂等命中）。
 */
export async function createOrder(matchId: number, quantity: number): Promise<OrderItem> {
  // 没有进行中的号才新生成 —— 保证「一次下单意图」对应「一个号」
  if (!pendingRequestId) {
    // crypto.randomUUID 在现代浏览器（含 localhost 安全上下文）原生可用，无需引库
    pendingRequestId = crypto.randomUUID()
  }
  const requestId = pendingRequestId
  try {
    const raw = await post<unknown>('/order/create', { matchId, quantity, requestId })
    pendingRequestId = null // 成功：这笔完成，下一笔换新号
    return parseOrder(raw, 0)
  } catch (e) {
    // 失败：保留 pendingRequestId，让「重试 / 用户再点」复用同一号 → 后端幂等命中
    throw e
  }
}

/** 支付（mock：推状态机 pending → paid） */
export async function payOrder(orderId: number): Promise<void> {
  await post<unknown>(`/order/${orderId}/pay`)
}

/** 取消（pending → closed；已支付的会被后端 3002 拒绝） */
export async function cancelOrder(orderId: number): Promise<void> {
  await post<unknown>(`/order/${orderId}/cancel`)
}
