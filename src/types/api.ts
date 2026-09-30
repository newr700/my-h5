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

/** 积分榜球队条目 */
export interface TeamStanding {
  /** 排名（1-20） */
  rank: number
  teamName: string
  /** 已赛轮次 */
  played: number
  win: number
  draw: number
  lose: number
  /** 进球数 */
  goalsFor: number
  /** 失球数 */
  goalsAgainst: number
  /** 积分 */
  points: number
  /**
   * 队徽图片地址 —— 注意类型文件描述的是「parse 归一化后的形状」，不是后端原文。
   * 后端给没给这个字段不确定（所以 parse 时用宽松模式），
   * 但我们保证归一化后它一定是 string：没图就是 ''，页面据此决定显示图片还是排名圆圈。
   */
  logoUrl: string
}
