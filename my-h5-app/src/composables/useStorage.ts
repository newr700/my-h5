import { ref, watch, type Ref } from 'vue'

/**
 * 全局可复用的组合式函数：一个自动和 localStorage 同步的 ref。
 * 逻辑复用统一用 useXxx 形式放这里，而不是在组件间复制粘贴。
 */
export function useStorage<T>(key: string, defaultValue: T): Ref<T> {
  const stored = localStorage.getItem(key)
  const data = ref<T>(stored ? (JSON.parse(stored) as T) : defaultValue) as Ref<T>

  watch(
    data,
    (val) => {
      localStorage.setItem(key, JSON.stringify(val))
    },
    { deep: true }
  )

  return data
}
