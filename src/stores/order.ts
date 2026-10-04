import { defineStore } from 'pinia'
import { ref } from 'vue'
import { cancelOrder, createOrder, fetchMatches, fetchOrders, payOrder } from '@/api/order'
import type { MatchInfo, OrderItem } from '@/types/api'

/**
 * 订单模块状态。
 *
 * 写操作后的状态同步策略：操作成功后重新拉取整个列表（loadOrders），
 * 而不是在本地手动改某一条 —— 本地改是「两处真相」的开始：
 * 你以为的状态和服务器真实状态可能不一致（比如另一端也在操作）。
 * 列表数据量小，重新拉一次的成本可忽略，一致性收益巨大。
 */
export const useOrderStore = defineStore('order', () => {
  const matches = ref<MatchInfo[]>([])
  const orders = ref<OrderItem[]>([])
  const loading = ref(false)
  const submitting = ref(false)

  async function loadMatches() {
    matches.value = await fetchMatches()
  }

  async function loadOrders() {
    loading.value = true
    try {
      const page = await fetchOrders(1, 50)
      orders.value = page.list
    } finally {
      loading.value = false
    }
  }

  /** 下单 → 刷新列表。submitting 防连点：网络慢时连点会重复下单 */
  async function submitOrder(matchId: number, quantity: number) {
    submitting.value = true
    try {
      await createOrder(matchId, quantity)
      await loadOrders()
    } finally {
      submitting.value = false
    }
  }

  async function pay(id: number) {
    await payOrder(id)
    await loadOrders()
  }

  async function cancel(id: number) {
    await cancelOrder(id)
    await loadOrders()
  }

  return { matches, orders, loading, submitting, loadMatches, loadOrders, submitOrder, pay, cancel }
})
