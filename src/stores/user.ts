import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchUserProfile } from '@/api/user'
import type { UserProfile } from '@/types/api'

/** 用户模块状态（码农 A 维护） */
export const useUserStore = defineStore('user', () => {
  const profile = ref<UserProfile | null>(null)
  const loading = ref(false)

  async function loadProfile() {
    loading.value = true
    try {
      profile.value = await fetchUserProfile()
    } finally {
      loading.value = false
    }
  }

  return { profile, loading, loadProfile }
})
