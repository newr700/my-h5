<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
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
 * 因为本页守卫保证已登录，且操作反馈用 ElMessage 即时给出 ——
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
    ElMessage.warning('请先选择比赛')
    return
  }
  try {
    await orderStore.submitOrder(selectedMatchId.value, quantity.value)
    ElMessage.success('下单成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '下单失败')
  }
}

async function onPay(id: number) {
  try {
    await orderStore.pay(id)
    ElMessage.success('支付成功')
  } catch (e) {
    // 后端状态机守卫会拒绝非法操作（如对 paid 订单再支付），错误话术来自后端
    ElMessage.error(e instanceof Error ? e.message : '支付失败')
  }
}

async function onCancel(id: number) {
  try {
    await orderStore.cancel(id)
    ElMessage.success('已取消')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '取消失败')
  }
}

/** 状态 → 标签类型（状态机的可视化）。
 *  el-tag 的 type 是预设语义色：warning 待支付 / success 已支付 / info 已关闭 */
const STATUS_TAG_TYPE: Record<string, 'warning' | 'success' | 'info'> = {
  pending: 'warning',
  paid: 'success',
  closed: 'info'
}
const STATUS_TEXT: Record<string, string> = {
  pending: '待支付',
  paid: '已支付',
  closed: '已关闭'
}
</script>

<template>
  <div class="page">
    <!-- 错误态：整页加载失败时给重试入口（清单：错误必须可感知） -->
    <el-alert
      v-if="error"
      type="error"
      :title="error"
      show-icon
      class="error-alert"
      @close="error = ''"
    >
      <template #default>
        <el-button size="small" type="danger" @click="error = ''; orderStore.loadMatches(); orderStore.loadOrders()">
          重试
        </el-button>
      </template>
    </el-alert>

    <!-- ── 买票区 ─────────────────────────────────────── -->
    <el-card class="block" header="买票">
      <el-radio-group v-model="selectedMatchId" class="match-group">
        <!-- Element Plus 的 el-radio 用 :value 绑定值（旧版用 label，已弃用） -->
        <el-radio
          v-for="match in orderStore.matches"
          :key="match.id"
          :value="match.id"
          border
          class="match-radio"
        >
          {{ match.matchTitle }} ｜ {{ match.matchTime }} ｜ ¥{{ formatPrice(match.unitPrice) }}/张
        </el-radio>
      </el-radio-group>

      <el-form :inline="true" class="buy-form">
        <el-form-item label="数量">
          <!-- 步进器上限 10 与后端 DTO 的 @Max(10) 一致 —— 双校验的可见例子 -->
          <el-input-number v-model="quantity" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="预计总价">
          <span class="price">¥{{ formatPrice(previewTotal) }}</span>
        </el-form-item>
      </el-form>

      <el-button
        type="primary"
        :loading="orderStore.submitting"
        :disabled="selectedMatchId === null"
        @click="onSubmit"
      >
        提交订单
      </el-button>
    </el-card>

    <!-- ── 我的订单 ────────────────────────────────────── -->
    <el-card class="block" header="我的订单">
      <el-empty v-if="!orderStore.orders.length && !orderStore.loading" description="还没有订单" />
      <el-table v-else :data="orderStore.orders" stripe>
        <el-table-column prop="matchTitle" label="比赛" min-width="160" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column label="单价" width="110">
          <template #default="{ row }">¥{{ formatPrice(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column label="总价" width="110">
          <template #default="{ row }">
            <span class="price">¥{{ formatPrice(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_TYPE[row.status] ?? 'info'" effect="light">
              {{ STATUS_TEXT[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <!-- 只有 pending 才显示操作按钮 —— 前端按状态机隐藏入口，
                 后端状态机守卫兜底（curl 绕过按钮直接调接口也会被 3002 拒） -->
            <template v-if="row.status === 'pending'">
              <el-button size="small" type="primary" @click="onPay(row.id)">支付</el-button>
              <el-button size="small" @click="onCancel(row.id)">取消</el-button>
            </template>
            <span v-else class="no-action">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
}
.block {
  max-width: 960px;
  margin: 0 auto 16px;
}
.error-alert {
  max-width: 960px;
  margin: 0 auto 16px;
}
/* el-radio 竖向排列更清晰：每个选项独占一行，比赛信息较长也不挤 */
.match-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}
.match-radio {
  width: 100%;
  margin-right: 0;
  height: auto;
  white-space: normal;
}
.buy-form {
  margin-top: 16px;
}
.price {
  color: #f56c6c;
  font-weight: 600;
}
.no-action {
  color: #c0c4cc;
}
</style>
