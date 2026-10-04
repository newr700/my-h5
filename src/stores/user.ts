import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchUserProfile, login as apiLogin, register as apiRegister } from '@/api/auth'
import type { UserProfile } from '@/types/api'

/**
 * 用户模块状态 —— 登录态的唯一管理者。
 *
 * ── token 存在哪，为什么是 localStorage ──────────────────
 * 候选有三个：内存（刷新就没）、localStorage、Cookie。
 * 选 localStorage 的理由：不被浏览器自动携带（Cookie 每次请求自动带，
 * 是 CSRF 的根源），且跨标签页共享、刷新不丢。
 * 代价必须说得出：任何能在页面执行 JS 的代码都能读到它 ——
 * 所以防 XSS 是保命前提（Vue 模板默认转义输出，正是在替你守这条线，
 * 这也是「非必要不用 v-html」的原因）。（技能点：XSS/CSRF 对比，面试必考）
 *
 * token 的读写只发生在三个地方：这个 store、request.ts 拦截器、这里 logout。
 * 页面永远不该直接碰 localStorage.getItem('token')。
 */

const TOKEN_KEY = 'token'

export const useUserStore = defineStore('user', () => {
  const profile = ref<UserProfile | null>(null)
  const loading = ref(false)

  // 计算属性：全项目判断「登录没」只看这一个，不散落各处 localStorage 直读
  const isLoggedIn = computed(() => !!localStorage.getItem(TOKEN_KEY))

  /** 登录：拿 token → 存 token → 拉资料。任何一步失败都不留半截状态 */
  async function login(username: string, password: string) {
    const result = await apiLogin(username, password)
    localStorage.setItem(TOKEN_KEY, result.token)
    profile.value = { id: result.id, username: result.username, nickname: result.nickname, createdAt: '' }
  }

  /** 注册（后端注册即登录，处理与 login 相同） */
  async function register(username: string, password: string, nickname: string) {
    const result = await apiRegister(username, password, nickname)
    localStorage.setItem(TOKEN_KEY, result.token)
    profile.value = { id: result.id, username: result.username, nickname: result.nickname, createdAt: '' }
  }

  /** 拉取完整资料（含注册时间），登录后或刷新页面后调用 */
  async function loadProfile() {
    loading.value = true
    try {
      profile.value = await fetchUserProfile()
    } finally {
      loading.value = false
    }
  }

  /**
   * 登出：本地清除即完成 —— 这正是 JWT 无状态的另一面：
   * 服务器不存会话，所以也没有「服务端登出」；token 在过期前理论上仍有效。
   * 要真正作废需要服务端黑名单（PRD 已知简化，面试要能讲出这个代价）。
   */
  function logout() {
    localStorage.removeItem(TOKEN_KEY)
    profile.value = null
  }

  return { profile, loading, isLoggedIn, login, register, loadProfile, logout }
})
