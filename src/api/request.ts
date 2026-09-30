import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import { isPlainObject } from '@/utils/validate'

/**
 * axios 唯一出口 —— 全项目不许在别处 new axios 实例。
 *
 * ── 为什么要“唯一” ─────────────────────────────────────
 * 如果各页面自己 import axios 各发各的，那么「带 token、拆外壳、统一报错」
 * 这些规矩就得在每个页面重复写一遍，改一处忘一处。
 * 收敛成唯一实例后，规矩只需要定一次，全项目自动生效 ——
 * 这就是清单里「全局资源收敛到唯一位置」在代码层的落点。
 *
 * ── 为什么 baseURL 从 .env 读而不是写死 ─────────────────
 * 开发时接口在 localhost:3000，上线后在真实域名；
 * 写死意味着每次发版都要改代码，漏改就是事故。
 * 环境变量让“代码”和“环境”分离 —— 清单「环境变量管理」条目。
 */
// ── 面试技能点（详见 docs/面试技能树.md）──────────────────
// 本文件：Axios 封装 / 拦截器 / Promise / HTTP 头与鉴权 / Partial<T>
// 考点速记：为什么全项目只此一个实例？拦截器按什么顺序执行？
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // 超时：10 秒拿不到响应就主动断开。
  // 没有超时的请求在断网时会永远挂着，loading 转圈转到天荒地老 —— C 世界不存在的问题
  timeout: 10000
})

// 请求拦截：自动带上 token。（技能点：Axios 拦截器 / HTTP 无状态与鉴权）
// 为什么每次都带？因为 HTTP 无状态，服务器不记得上一个请求是谁发的，
// token 相当于每次调用都随身携带的「身份证明」（类比：每次都传 context 结构体指针）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：拆掉外层 { code, message, data }，业务代码只拿到 data
// 返回 any 是因为我们改变了拦截器的返回结构（AxiosResponse → 业务 data）
request.interceptors.response.use(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (response): any => {
    const res = response.data

    // 先校验外壳本身。以前直接写 res.code !== 0 有个坑：
    // 后端忘了返回 code 时，undefined !== 0 也成立，页面只看到一句莫名其妙的「请求失败」
    // 现在能明确告诉你到底是外壳的哪个字段出了问题
    if (!isPlainObject(res)) {
      return Promise.reject(
        new Error(`[契约校验] 响应应该是一个对象，实际收到 ${String(res)?.slice(0, 120)}`)
      )
    }

    // Partial<> 表示「每个字段都可能有也可能没有」——这正是我们要检查的事情，
    // 所以这里不能用 ApiResponse 直接断言，那等于先假设它就是对的
    // （技能点：TS utility types——Partial 的实战用法，面试爱问）
    const shell = res as Partial<ApiResponse>

    if (typeof shell.code !== 'number') {
      return Promise.reject(new Error(`[契约校验] 响应缺少数字类型的 code 字段，实际是 ${String(shell.code)}`))
    }
    if (shell.code !== 0) {
      // 业务错误在这里统一抛出，页面层 catch 即可
      return Promise.reject(new Error(shell.message || '请求失败'))
    }
    if (shell.data === undefined) {
      return Promise.reject(new Error('[契约校验] 响应缺少 data 字段'))
    }
    return shell.data
  },
  (error: AxiosError) => {
    // 网络错误 / HTTP 非 2xx：原样往外抛，让页面层决定怎么提示。
    // 这里不弹 toast 的原因：列表页和表单页对错误的呈现方式完全不同
    // （全屏错误态 vs 输入框旁红字），请求层不该越权替页面做决定
    return Promise.reject(error)
  }
)

/**
 * get / post：给 request 套上泛型的薄封装。
 *
 * 为什么不直接用 request.get？
 * 1. 业务代码只 import 这两个函数，永远碰不到 request 实例本身，
 *    「唯一出口」的约束靠这个才真正成立；
 * 2. 泛型 T 由调用方填写后，返回值直接是 Promise<T>，
 *    页面/store 里不需要再写 as 断言。
 *
 * 注意：这里的 as unknown as T 不是“转换”而是“承诺”——
 * T 说的是「后端应该给我什么」。承诺靠不靠得住，
 * 由各模块 api 文件里的 parse 函数（运行时校验）保证，不靠这两行。
 * （技能点：TS 泛型与类型断言——unknown 和 any 的区别是高频考题）
 */
export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return (await request.get(url, config)) as unknown as T
}

export async function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return (await request.post(url, data, config)) as unknown as T
}
