import { get } from './request'
import type { TeamStanding } from '@/types/api'

/**
 * 积分榜模块的所有接口（样例模块，供参考模仿）
 *
 * ② 接口层：页面不许直接写 axios，所有请求都收敛成这样的函数。
 * 后端做好 /standings 接口后，这里一行都不用改。
 */
export function fetchStandings() {
  return get<TeamStanding[]>('/standings')
}
