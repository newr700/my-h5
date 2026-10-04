<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { useOrderStore } from '@/stores/order'
import { formatPrice } from '@/utils/format'

/**
 * 订单页（PRD 5.2 写操作闭环的完整演示）。
 *
 * 页面结构就是闭环本身：
 *   上半区「买票」：选比赛 → 选数量 → 提交（金额预览只是预览，成交价以后端为准）
 *   下半区「我的订单」：列表 → 支付/取消（推状态机）
 *
 * 与积分榜页相同的三态处理（骨架/错误/空态）这里从简成两态，
 * 因为本页守卫保证已登录，且操作反馈用 toast 即时给出 ——
 * 三态不是教条，按页面性质取舍。
 */
const orderStore = useOrderStore()

const selectedMatchId = ref<number | null>(null)
const quantity = ref(1)
const error = ref('')

onMounted(async () => {
  try {
    await Promise.all([orderStore.loadMatches(), orderStore.loadOrders()])
    // 默认选中第一场，减少一次点击
    if (orderStore.matches.length > 0) {
      selectedMatchId.value = orderStore.matches[0].id
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
})

/** 选中的比赛对象（预览用） */
const selectedMatch = computed(() =>
  orderStore.matches.find((m) => m.id === selectedMatchId.value) ?? null
)

/**
 * 预计总价 —— 注意这只是「预览」，不是成交价。
 * 真正入账的金额由后端拿 matchId 查库重算（PRD 5.2），
 * 就算这里显示错了，订单金额也是对的 —— 这正是「后端重算」的意义。
 */
const previewTotal = computed(() =>
  selectedMatch.value ? selectedMatch.value.unitPrice * quantity.value : 0
)

async function onSubmit() {
  if (selectedMatchId.value === null) {
    showToast('请先选择比赛')
    return
  }
  try {
    await orderStore.submitOrder(selectedMatchId.value, quantity.value)
    showToast('下单成功')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '下单失败')
  }
}

async function onPay(id: number) {
  try {
    await orderStore.pay(id)
    showToast('支付成功')
  } catch (e) {
    // 后端状态机守卫会拒绝非法操作（如对 paid 订单再支付），错误话术来自后端
    showToast(e instanceof Error ? e.message : '支付失败')
  }
}

async function onCancel(id: number) {
  try {
    await orderStore.cancel(id)
    showToast('已取消')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '取消失败')
  }
}

/** 状态 → 展示文案与颜色（状态机的可视化） */
const STATUS_META: Record<string, { text: string; color: string }> = {
  pending: { text: '待支付', color: '#ff976a' },
  paid: { text: '已支付', color: '#07c160' },
  closed: { text: '已关闭', color: '#c8c9cc' }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="订单" />

    <!-- 错误态：整页加载失败时给重试入口（清单：错误必须可感知） -->
    <van-empty v-if="error" :description="error">
      <van-button type="primary" round @click="() => { error = ''; orderStore.loadMatches(); orderStore.loadOrders() }">
        重试
      </van-button>
    </van-empty>

    <template v-else>
      <!-- ── 买票区 ─────────────────────────────────────── -->
      <van-cell-group inset title="买票">
        <van-cell
          v-for="match in orderStore.matches"
          :key="match.id"
          :title="match.matchTitle"
          :label="`${match.matchTime} ｜ ¥${formatPrice(match.unitPrice)}/张`"
          @click="selectedMatchId = match.id"
        >
          <template #right-icon>
            <van-radio :model-value="selectedMatchId === match.id" />
          </template>
        </van-cell>

        <van-cell title="数量">
          <template #value>
            <!-- 步进器上限 10 与后端 DTO 的 @Max(10) 一致 —— 双校验的可见例子 -->
            <van-stepper v-model="quantity" min="1" max="10" />
          </template>
        </van-cell>

        <van-cell title="预计总价">
          <template #value>
            <span class="price">¥{{ formatPrice(previewTotal) }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <div class="submit-area">
        <van-button
          type="primary"
          block
          round
          :loading="orderStore.submitting"
          :disabled="selectedMatchId === null"
          @click="onSubmit"
        >
          提交订单
        </van-button>
      </div>

      <!-- ── 我的订单 ────────────────────────────────────── -->
      <van-cell-group inset title="我的订单">
        <van-empty v-if="!orderStore.orders.length && !orderStore.loading" description="还没有订单" />
        <van-cell
          v-for="order in orderStore.orders"
          :key="order.id"
          :title="order.matchTitle"
          :label="`${order.orderNo} ｜ ${order.quantity} 张 × ¥${formatPrice(order.unitPrice)}`"
        >
          <template #value>
            <div class="order-right">
              <span class="price">¥{{ formatPrice(order.totalAmount) }}</span>
              <van-tag :color="STATUS_META[order.status]?.color" plain>
                {{ STATUS_META[order.status]?.text ?? order.status }}
              </van-tag>
              <!-- 只有 pending 才显示操作按钮 —— 前端按状态机隐藏入口，
                   后端状态机守卫兜底（curl 绕过按钮直接调接口也会被 3002 拒） -->
              <div v-if="order.status === 'pending'" class="order-actions">
                <van-button size="small" type="primary" @click="onPay(order.id)">支付</van-button>
                <van-button size="small" @click="onCancel(order.id)">取消</van-button>
              </div>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
    </template>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 24px;
}
.price {
  color: #ee0a24;
  font-weight: 600;
}
.submit-area {
  margin: 16px;
}
.order-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.order-actions {
  display: flex;
  gap: 8px;
}

/* PC 断点：规则与积分榜页一致 */
@media (min-width: 768px) {
  .submit-area {
    max-width: 320px;
    margin: 24px auto;
  }
  .order-actions {
    flex-direction: row;
  }
  .page :deep(.van-cell:hover) {
    background: #fafafa;
  }
}
</style>
