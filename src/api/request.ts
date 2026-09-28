import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'

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
    const res = response.data as ApiResponse
    if (res.code !== 0) {
      // 业务错误在这里统一抛出，页面层 catch 即可
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
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
