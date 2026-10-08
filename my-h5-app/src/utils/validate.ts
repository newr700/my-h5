/**
 * 运行时校验工具（零依赖，纯函数）
 *
 * ── 为什么需要这份代码 ──────────────────────────────
 * TypeScript 的类型只在「写代码 + 编译时」存在，浏览器跑起来后一行都不剩。
 * 所以后端万一返回了错的字段类型、少返回一个字段、数组少给几条，
 * TS 是拦不住的 —— 只有这份代码能在数据进门的那一刻把它拦下来。
 *
 * ── 用在哪个位置 ────────────────────────────────────
 * 只在 api/模块.ts 里用（数据刚拿到时）。
 * 不要放到 views 里 —— 那里是画界面的，不该掺校验逻辑。
 *
 * ── 两种力度怎么选 ──────────────────────────────────
 * asNumber(raw, path)         严格：类型不对就抛错，页面显示加载失败
 * asNumber(raw, path, 0)      宽松：类型不对就用兜底值 0，开发时控制台告警
 * 业务核心数据（金额、数量、ID）用严格，展示类次要字段（头像、描述）用宽松。
 */

/** 把收到的值描述成人能看懂的样子，专供报错信息使用 */
function describe(raw: unknown): string {
  if (raw === null) return 'null'
  if (raw === undefined) return 'undefined（这个字段根本没返回）'
  if (Array.isArray(raw)) return `数组(共 ${raw.length} 项)`
  if (typeof raw === 'object') return '对象'
  return `${typeof raw} 类型的 ${String(raw)}`
}

/** 只在开发环境告警，生产环境保持安静 */
function warnOnce(message: string): void {
  if (import.meta.env.DEV) {
    console.warn(message)
  }
}

/**
 * 失败时的统一出口：给了兜底值就走宽松模式，没给就严格抛错
 *
 * 宽松模式再细分两种：
 * - 字段缺失（undefined / null）：这是「可选字段」的正常情况，静默用兜底值
 * - 字段存在但类型不对：后端可能改错了，开发环境必须告警
 */
function fail(path: string, expected: string, actual: unknown, fallback?: unknown): never | unknown {
  const message = `[契约校验] ${path} 应该是 ${expected}，实际收到 ${describe(actual)}`
  if (fallback !== undefined) {
    if (actual !== undefined && actual !== null) {
      warnOnce(`${message}，已用兜底值代替`)
    }
    return fallback
  }
  throw new Error(message)
}

/** 是不是一个普通对象（typeof null 和数组也都是 'object'，所以要排掉）
 * （技能点：TS 类型守卫——raw is Record<string, unknown> 的返回值写法，
 *  能让 TS 在 if 之后自动收窄类型，这是 any 做不到的） */
export function isPlainObject(raw: unknown): raw is Record<string, unknown> {
  return typeof raw === 'object' && raw !== null && !Array.isArray(raw)
}

/** 校验并返回字符串 */
export function asString(raw: unknown, path: string, fallback?: string): string {
  if (typeof raw === 'string') return raw
  return fail(path, '字符串', raw, fallback) as string
}

/** 校验并返回数字（NaN 和 Infinity 不算有效数字） */
export function asNumber(raw: unknown, path: string, fallback?: number): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw
  return fail(path, '数字', raw, fallback) as number
}

/** 校验并返回布尔值 */
export function asBoolean(raw: unknown, path: string, fallback?: boolean): boolean {
  if (typeof raw === 'boolean') return raw
  return fail(path, '布尔值', raw, fallback) as boolean
}

/**
 * 校验并返回枚举值之一，例如订单状态只允许 'pending' | 'paid' | 'closed'
 * 后端要哪天多返回一个 'refunded'，这里立刻报错，不会漏到页面上显示空白
 */
export function asEnum<T extends string>(raw: unknown, allowed: readonly T[], path: string, fallback?: T): T {
  if (typeof raw === 'string' && allowed.includes(raw as T)) return raw as T
  return fail(path, `${allowed.join(' | ')} 之一`, raw, fallback) as T
}

/** 校验并返回数组（逐项怎么校验，交给外面 map 处理） */
export function asArray(raw: unknown, path: string): unknown[] {
  if (Array.isArray(raw)) return raw
  // 数组没有兜底值的说法，少数据就是少数据，必须抛错让页面感知
  throw new Error(`[契约校验] ${path} 应该是数组，实际收到 ${describe(raw)}`)
}

/**
 * 校验数量 —— 这就是你说的「需要特定数量的数据」
 *
 * @param rule 传数字＝必须正好这么多条；传对象＝限制范围
 * @example expectCount(list, 'standings', 20)              // 必须恰好 20 条
 * @example expectCount(list, 'orderList', { min: 1 })       // 至少 1 条
 * @example expectCount(list, 'hotList', { max: 10 })        // 最多 10 条
 */
export function expectCount(
  list: unknown[],
  path: string,
  rule: number | { min?: number; max?: number }
): void {
  const actual = list.length
  if (typeof rule === 'number') {
    if (actual !== rule) {
      throw new Error(`[契约校验] ${path} 应该有 ${rule} 条数据，实际收到 ${actual} 条`)
    }
    return
  }
  const { min, max } = rule
  if (min !== undefined && actual < min) {
    throw new Error(`[契约校验] ${path} 至少要有 ${min} 条，实际只有 ${actual} 条`)
  }
  if (max !== undefined && actual > max) {
    throw new Error(`[契约校验] ${path} 最多 ${max} 条，实际收到 ${actual} 条`)
  }
}
