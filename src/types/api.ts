/**
 * 全局类型定义 —— 与后端接口文档一一对齐的「契约镜像」。
 * 后端字段改了，先改这里，TS 会帮你找出前端所有受影响的地方。
 */

/** 后端统一响应结构 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 用户信息 */
export interface UserProfile {
  id: number
  nickname: string
  avatar: string
}

/** 订单条目 */
export interface OrderItem {
  id: number
  title: string
  /** 金额（分） */
  amount: number
  status: 'pending' | 'paid' | 'closed'
}
