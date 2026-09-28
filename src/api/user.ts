import { get } from './request'
import type { UserProfile } from '@/types/api'

/** 用户模块的所有接口（码农 A 维护） */
export function fetchUserProfile() {
  return get<UserProfile>('/user/profile')
}
