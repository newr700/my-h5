import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchUserProfile, login as apiLogin, register as apiRegister } from '@/api/auth'
import { applyExpert as apiApplyExpert } from '@/api/user'
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

/** 行业专家的门槛等级 —— 与后端 user/UserLevels.java 的 EXPERT 保持一致 */
const USER_LEVEL_EXPERT = 2

export const useUserStore = defineStore('user', () => {
  const profile = ref<UserProfile | null>(null)
  const loading = ref(false)

  // 计算属性：全项目判断「登录没」只看这一个。
  // ⚠️ 关键修复点（旧实现是 !!localStorage.getItem(TOKEN_KEY)）：localStorage 不是 Vue 的响应式数据源，
  //   computed 读它不会建立响应式依赖，登录后 setItem 改了 localStorage 也不会触发重算，
  //   于是右上角的 v-if="isLoggedIn" 永远停在「未登录」（这正是「登录后还显示未登录」的 bug）。
  //   改为依赖响应式的 profile：登录后 profile 被赋值即变 true，登出时 profile 置空即变 false。
  const isLoggedIn = computed(() => !!profile.value)

  /**
   * 是否是行业专家（Lv.2）—— 决定「权威解析」页要不要显示评论输入框。
   *
   * ⚠️ 这只是【界面层】的隐藏，不是权限控制。
   * 真正的判定在后端 addComment 里做：就算有人改 JS 把输入框显示出来，
   * 发过去的请求照样会被后端的等级守卫拦下并返回 6002。
   */
  const isExpert = computed(() => (profile.value?.userLevel ?? 1) >= USER_LEVEL_EXPERT)

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
   * 申请成为行业专家（V7 新增）。
   *
   * 成功后【必须重新拉取资料】，而不是把本地 userLevel 直接改成 2 ——
   * 本地改一个数字固然简单，但那是「假装数据库已经变了」，
   * 刷新页面立刻穿帮。写操作和读回填分离，状态才不会漂移。
   */
  async function applyExpert() {
    loading.value = true
    try {
      await apiApplyExpert()
      await loadProfile()
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

  /**
   * 启动 / 刷新页面后恢复登录态：本地有 token 就拉一次资料把 profile 补回来，
   * 否则刷新后 store 重建、profile 为 null，isLoggedIn 又会变回 false（「已登录却显示未登录」）。
   * token 失效（1101/1102）由 request.ts 拦截器统一清 token + 跳登录，这里静默吞掉即可。
   */
  async function restore() {
    if (localStorage.getItem(TOKEN_KEY)) {
      try {
        await loadProfile()
      } catch {
        /* 交给拦截器处理 */
      }
    }
  }

  return {
    profile,
    loading,
    isLoggedIn,
    isExpert,
    login,
    register,
    loadProfile,
    applyExpert,
    logout,
    restore
  }
})
