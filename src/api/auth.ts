import { get, post } from './request'
import { asNumber, asString, isPlainObject } from '@/utils/validate'
import type { LoginResult, UserProfile } from '@/types/api'

/**
 * 认证模块接口（对应 Java 端 AuthController / UserController）。
 *
 * 模式与 standings.ts 完全一致：get/post 的泛型只写 unknown，
 * 拿到数据先 parse 校验再用 —— 「先检查再使用」的规矩在所有模块通用。
 */

function parseLoginResult(raw: unknown): LoginResult {
  if (!isPlainObject(raw)) {
    throw new Error('[契约校验] auth 响应应该是一个对象')
  }
  return {
    // token 是后续所有请求的通行证，错了整个登录态都是错的 —— 严格模式
    token: asString(raw.token, 'auth.token'),
    id: asNumber(raw.id, 'auth.id'),
    username: asString(raw.username, 'auth.username'),
    nickname: asString(raw.nickname, 'auth.nickname')
  }
}

/** 注册（成功即登录，后端直接返回 token） */
export async function register(username: string, password: string, nickname: string): Promise<LoginResult> {
  const raw = await post<unknown>('/auth/register', { username, password, nickname })
  return parseLoginResult(raw)
}

/** 登录 */
export async function login(username: string, password: string): Promise<LoginResult> {
  const raw = await post<unknown>('/auth/login', { username, password })
  return parseLoginResult(raw)
}

/** 获取当前登录用户资料（需要 token，由 request.ts 拦截器自动携带） */
export async function fetchUserProfile(): Promise<UserProfile> {
  const raw = await get<unknown>('/user/profile')
  if (!isPlainObject(raw)) {
    throw new Error('[契约校验] user/profile 应该是一个对象')
  }
  return {
    id: asNumber(raw.id, 'profile.id'),
    username: asString(raw.username, 'profile.username'),
    nickname: asString(raw.nickname, 'profile.nickname'),
    createdAt: asString(raw.createdAt, 'profile.createdAt'),
    // 头像属于「展示类次要字段」：缺失/类型错时用兜底值 ''，不阻断资料加载
    avatarUrl: asString(raw.avatarUrl, 'profile.avatarUrl', '')
  }
}
