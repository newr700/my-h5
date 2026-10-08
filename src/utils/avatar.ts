/**
 * 头像 URL 拼装 —— 把后端返回的相对路径变成 <img> 能直接用的地址。
 *
 * ── 为什么需要它 ───────────────────────────────────────
 * 后端只存相对路径（如 /uploads/12_xxx.png），不存域名（环境分离原则：代码里不写死域名）。
 * 但浏览器 <img src> 需要能直接访问的地址，这里按部署环境补成完整地址：
 *
 *   - 开发（VITE_API_BASE_URL=/api，相对基址）：直接返回相对路径，
 *     由 vite.config.ts 的 /uploads 代理转发到后端 8080；
 *   - 生产（VITE_API_BASE_URL=https://api.example.com）：拼成绝对 URL。
 *
 * 一套代码，两种部署都正确。
 */
export function avatarSrc(url?: string | null): string {
  if (!url) return ''
  // 已经是完整地址（如别人传进来的绝对 URL），原样返回
  if (/^https?:\/\//.test(url)) return url
  const base = import.meta.env.VITE_API_BASE_URL as string | undefined
  if (base && base.startsWith('http')) {
    try {
      return new URL(url, base).href
    } catch {
      return url
    }
  }
  // 相对基址（dev）：直接返回相对路径，靠 vite 代理转发
  return url
}
