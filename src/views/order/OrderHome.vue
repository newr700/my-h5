<script setup lang="ts">
import { onMounted } from 'vue'
import { useOrderStore } from '@/stores/order'
import { formatPrice } from '@/utils/format'

const orderStore = useOrderStore()

onMounted(() => {
  orderStore.loadOrders().catch(() => {})
})
</script>

<template>
  <div class="page">
    <van-nav-bar title="订单列表" />
    <van-cell
      v-for="order in orderStore.orders"
      :key="order.id"
      :title="order.title"
      :value="`¥${formatPrice(order.amount)}`"
      :label="order.status"
    />
    <van-empty v-if="!orderStore.orders.length" description="后端还没起，先看个壳" />
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}
</style>
