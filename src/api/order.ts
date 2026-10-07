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
 */
export async function createOrder(matchId: number, quantity: number): Promise<OrderItem> {
  const raw = await post<unknown>('/order/create', { matchId, quantity })
  return parseOrder(raw, 0)
}

/** 支付（mock：推状态机 pending → paid） */
export async function payOrder(orderId: number): Promise<void> {
  await post<unknown>(`/order/${orderId}/pay`)
}

/** 取消（pending → closed；已支付的会被后端 3002 拒绝） */
export async function cancelOrder(orderId: number): Promise<void> {
  await post<unknown>(`/order/${orderId}/cancel`)
}
