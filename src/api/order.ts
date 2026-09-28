import { get } from './request'
import type { OrderItem } from '@/types/api'

/** 订单模块的所有接口（码农 B 维护） */
export function fetchOrderList() {
  return get<OrderItem[]>('/order/list')
}
