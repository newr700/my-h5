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
  /** 头像相对路径（如 /uploads/12_xxx.png）；空串表示未设置，前端显示首字母占位 */
  avatarUrl?: string
  /**
   * 用户等级 —— V7 新增（对应后端 user/UserLevels.java）
   * 1 = 普通球迷（NORMAL），2 = 行业专家（EXPERT）。
   * 只有 Lv.2 以上能在「权威解析」页发表评论。
   *
   * 注意：前端拿它只决定【要不要显示评论输入框】（体验层）；
   * 真正的拦人由后端等级守卫完成 —— 前端的锁防君子，后端的锁防所有人。
   */
  userLevel?: number
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

  // ── V2 第一步：库存快照（对应 Java MatchVo 的 totalStock / stock）────────

  /** 总票数 —— 用于展示「共 N 张」，也是剩余票数的参照上限 */
  totalStock: number

  /**
   * 剩余票数 —— ⚠️ 这是【某一瞬间的快照】，不是承诺。
   *
   * 列表返回的余票，在「我们看到数字」到「用户点击提交」之间可能已经变了：
   * 网络传输要几百毫秒，够别人把最后几张买走了。
   *
   * 所以它的正确用法只有两个：
   *   ✅ 控制 UI —— 显示「余 N 张」、售罄时把选项置灰禁点；
   *   ❌ 不能做业务判断 —— 不能因为「显示还剩 3 张」就认为一定买得到。
   *
   * 真正的把关在后端：下单那一刻会走一条带条件的原子 UPDATE
   * （详见后端 MatchInfoMapper.deductStock 的注释），不够就返回错误码 3003。
   * 这条边界就是工程手册里那句「前端负责展示，后端负责正确」。
   */
  stock: number
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

// ══════════════════════════════════════════════════════════════════
// 英超预测模块（V7 新增）
// 对应后端 com.myh5.server.prediction 包下的各 VO
// ══════════════════════════════════════════════════════════════════

/**
 * 球队简要信息（对应 prediction/vo/TeamBriefVo.java）
 *
 * 凡是「显示队徽 + 队名」的地方都复用它：历届战绩的三个名次、夺冠次数榜、AI 预测的球队行。
 * 抽出来的价值是「契约只写一次」——以后加字段三处同时生效，不会漏掉某一处显示不出来。
 *
 * 契约要点：
 * - 颜色是 #RRGGBB 字符串，直接就是合法的 CSS 颜色值，前端不用再做映射；
 * - 没图时 logoUrl 是 '' 而不是 null，前端靠它决定「显示图片还是兜底图形」。
 */
export interface TeamBrief {
  teamName: string
  /** 三字母缩写（ARS / MCI …），队徽上的文字兜底就靠它 */
  shortName: string
  colorPrimary: string
  colorSecondary: string
  logoUrl: string
}

/**
 * 一个维度的分数（对应 vo/DimScoreVo.java）
 *
 * 同一个形状代表两种含义，靠接口路径区分，不重复定义两个类型：
 * - /teams 里：这支球队在该维度的得分
 * - /algorithm 里：该维度的权重（合计 100）
 */
export interface DimScore {
  /** 维度 key，如 'history' / 'star' */
  key: string
  /** 中文名，如 '历史夺冠次数' */
  label: string
  score: number
}

/** 明星球员（对应 vo/StarPlayerVo.java） */
export interface StarPlayer {
  playerName: string
  position: string
  jerseyNumber: number
  /** 没照片时是 ''，前端用「球衣号 + 姓名首字」圆牌兜底 */
  photoUrl: string
}

/** AI 预测条目（对应 vo/TeamPredictionVo.java） */
export interface TeamPrediction {
  /** 排名 —— 后端生成（列表已按概率倒序），前端直接显示，不自己算 */
  rank: number
  teamName: string
  teamNameEn: string
  shortName: string
  colorPrimary: string
  colorSecondary: string
  logoUrl: string
  /**
   * 夺冠概率百分数（0~100）—— 由后端按六维加权现算出来的，不是数据库里的字段。
   * 存一份「算好的概率」等于存了两份可能打架的真相，所以这里只传结果不存底稿。
   */
  winProbability: number
  /** 六个维度的得分，顺序 = 雷达图六个顶点的顺时针顺序，照顺序画即可 */
  dims: DimScore[]
  /** 可能是空数组（只有头部球队配了球员），页面必须能优雅显示空态 */
  starPlayers: StarPlayer[]
}

/** 专家观点卡片（对应 vo/ExpertAnalysisVo.java） */
export interface ExpertAnalysis {
  id: number
  /** 专家英文名，如 Martin Tyler */
  nameEn: string
  /** 专家中文名，如 马丁·泰勒 —— 页面上的「中/英切换」切的就是用哪个字段 */
  nameCn: string
  /** 他支持的那支球队（中文名） */
  teamName: string
  teamNameEn: string
  shortName: string
  colorPrimary: string
  colorSecondary: string
  logoUrl: string
  /** 支持这支队的理由 */
  reason: string
  /** 没头像时是 ''，前端用姓名首字母占位 */
  avatarUrl: string
  /** 已有评论条数 —— 后端一次 IN 聚合查回来的，不是前端数出来的 */
  commentCount: number
}

/** 专家评论（对应 vo/ExpertCommentVo.java） */
export interface ExpertComment {
  id: number
  analysisId: number
  /** 可能为 null：种子数据里的演示评论没有真实用户 */
  userId: number | null
  nickname: string
  /** 【发布当时】的等级快照 —— 事后降级不该改写这条评论的等级标记 */
  userLevel: number
  content: string
  createdAt: string
}

/** 历届战绩一行（对应 vo/SeasonHistoryVo.java） */
export interface SeasonHistory {
  /** 届数（第 33 届），官方口径 */
  edition: number
  /** 赛季起始年份，如 2024 */
  seasonYear: number
  /** 人看球习惯的说法，如 "2024-25"；这个拼接由后端做好，前端不自己拼 */
  seasonLabel: string
  champion: TeamBrief
  runnerUp: TeamBrief
  third: TeamBrief
}

/** 夺冠次数榜一项（对应 vo/TitleCountVo.java） */
export interface TitleCount {
  team: TeamBrief
  count: number
}
