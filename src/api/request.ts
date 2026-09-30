import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import { isPlainObject } from '@/utils/validate'

/**
 * axios 唯一出口 —— 全项目不许在别处 new axios 实例。
 * 鉴权、错误提示、响应结构拆包，全部在这里统一处理。
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

// 请求拦截：自动带上 token
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
    // 网络错误 / HTTP 非 2xx
    return Promise.reject(error)
  }
)

export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return (await request.get(url, config)) as unknown as T
}

export async function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return (await request.post(url, data, config)) as unknown as T
}
