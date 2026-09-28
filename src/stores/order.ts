import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchOrderList } from '@/api/order'
import type { OrderItem } from '@/types/api'

/** 订单模块状态（码农 B 维护） */
export const useOrderStore = defineStore('order', () => {
  const orders = ref<OrderItem[]>([])
  const loading = ref(false)

  async function loadOrders() {
    loading.value = true
    try {
      orders.value = await fetchOrderList()
    } finally {
      loading.value = false
    }
  }

  return { orders, loading, loadOrders }
})
