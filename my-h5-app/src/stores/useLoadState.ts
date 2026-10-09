import { ref } from 'vue'

/**
 * 三个预测页面的 store 共用的「首次/刷新」加载状态机。
 *
 * ── 三态约定（沿用 stores/standings.ts）────────────────────
 *   加载中 loading     首次进入，一条数据都没有 → 骨架屏
 *   刷新中 refreshing  已有数据，正在更新 → 不打断浏览
 *   加载失败 error     只有「一条数据都没有」时才占据全屏
 * 失败信息永远如实展示 —— 不塞假数据让页面假装很热闹。
 */
export function useLoadState() {
  const loading = ref(false)
  const refreshing = ref(false)
  const error = ref('')

  /** 包装一次加载：负责开关 loading/error，并把异常交回给调用方处理 */
  async function run<T>(task: () => Promise<T>, hasData: () => boolean): Promise<T | undefined> {
    const firstTime = !hasData()
    if (firstTime) {
      loading.value = true
      error.value = ''
    } else {
      refreshing.value = true
    }
    try {
      return await task()
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err)
      console.error('[prediction] 加载失败：', msg)
      // 已有数据时刷新失败：不动 error，避免整页变错误态，交给页面弹轻提示
      if (firstTime) error.value = msg
      throw err
    } finally {
      loading.value = false
      refreshing.value = false
    }
  }

  return { loading, refreshing, error, run }
}
