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

/* PC 断点：规则与积分榜页一致，见 StandingsList.vue 里的详细说明 */
@media (min-width: 768px) {
  /* :deep() 是 scoped 样式的逃生口：van-cell 是 Vant 内部元素，
     拿不到本组件的 data-v-xxx 属性，不加 :deep() 这条选择器永远命中不了。
     （技能点：scoped 原理与深度选择器，面试常问「怎么改子组件/组件库样式」） */
  .page :deep(.van-cell:hover) {
    background: #fafafa;
  }
}
</style>
