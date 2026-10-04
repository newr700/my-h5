/**
 * 全局类型定义 —— 与后端接口文档一一对齐的「契约镜像」。
 * 后端字段改了，先改这里，TS 会帮你找出前端所有受影响的地方。
 *
 * 当前契约方：my-h5-server（Java / Spring Boot），
 * 各 VO 类与本文件一一对应（每个类型上方都标了对面的类名）。
 */

/** 后端统一响应结构（对应 Java 端 common/Result.java） */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页响应（对应 common/PageResult.java，工程手册 4.3 裁定的形状） */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 个人资料（对应 user/UserProfileVo.java） */
export interface UserProfile {
  id: number
  username: string
  nickname: string
  /** "yyyy-MM-dd HH:mm:ss" 字符串（工程手册 4.3：时间一律字符串，东八区） */
  createdAt: string
}

/** 登录/注册成功的响应（对应 auth/vo/LoginVo.java） */
export interface LoginResult {
  token: string
  id: number
  username: string
  nickname: string
}

/** 在售比赛（对应 match/MatchVo.java） */
export interface MatchInfo {
  id: number
  matchTitle: string
  matchTime: string
  /** 单价（分）—— 金额全程整数，展示时才除以 100（见 utils/format.ts） */
  unitPrice: number
}

/** 订单条目（对应 order/OrderVo.java） */
export interface OrderItem {
  id: number
  /** 业务订单号（MO 开头），给人看的；id 是自增主键，不对外暴露业务量 */
  orderNo: string
  matchTitle: string
  matchTime: string
  quantity: number
  /** 单价（分），下单时的快照 */
  unitPrice: number
  /** 总价（分），后端重算的结果 */
  totalAmount: number
  /** 状态机：pending → paid / closed（closed 是终态） */
  status: 'pending' | 'paid' | 'closed'
  createdAt: string
}

/** 积分榜球队条目（对应 standings/StandingVo.java） */
export interface TeamStanding {
  /** 排名（1-20），后端生成 */
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
