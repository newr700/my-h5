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

// ── V2 第一步：库存相关的 UI 状态 ──────────────────────────────

/**
 * 可买数量上限 = min(后端单笔上限 10, 当前剩余库存)。
 *
 * 【这是一个体验优化，不是安全措施】—— 这点务必分清：
 * 用户在步进器里选不出超过库存的数量，确实少撞几次后端 3003，
 * 但这个限制【可以被绕过】：改请求体直接发 quantity=10 的程序，
 * 或者票在这几百毫秒里被别人买走，前端的限制都拦不住。
 * 真正拦得住的只有后端那条原子 UPDATE。
 *
 * 把「前端限制」当安全手段，是典型的层次错位 ——
 * 前端的所有验证，价值都在于【让用户少走弯路】，不在于防攻击。
 */
const maxBuyable = computed(() => {
  if (!selectedMatch.value) return 1
  return Math.max(1, Math.min(10, selectedMatch.value.stock))
})

/** 是否已售罄（余票为 0）—— 用于给按钮和选项加视觉反馈 */
const soldOut = computed(() => selectedMatch.value !== null && selectedMatch.value.stock <= 0)

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
  if (soldOut.value) {
    ElMessage.warning('该场比赛已售罄')
    return
  }
  try {
    await orderStore.submitOrder(selectedMatchId.value, quantity.value)
    ElMessage.success('下单成功')
    // 【V2】下完单必须重新拉一次比赛列表刷新余票。
    // 不刷新的话页面上还是下单前的旧数字，用户接着买第二单就会撞上 3003 莫名其妙。
    // 这是「状态一致性」的前端版本：数据变了就重新取，不要在本地推算。
    await orderStore.loadMatches()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '下单失败')
    // 失败也要刷新 —— 尤其是 3003 库存不足，原因通常就是「票被别人买走了」，
    // 不刷新的话用户会盯着过期的数字反复撞同一个错
    await orderStore.loadMatches()
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
          :disabled="match.stock <= 0"
          border
          class="match-radio"
        >
          {{ match.matchTitle }} ｜ {{ match.matchTime }} ｜ ¥{{ formatPrice(match.unitPrice) }}/张
          <!-- 余票提示：紧张时（≤5）转橙色，售罄转红色 -->
          <span v-if="match.stock > 0" class="stock" :class="{ warn: match.stock <= 5 }">
            ｜余 {{ match.stock }} 张
          </span>
          <span v-else class="stock sold-out">｜已售罄</span>
        </el-radio>
      </el-radio-group>

      <el-form :inline="true" class="buy-form">
        <el-form-item label="数量">
          <!-- 步进器上限跟随剩余库存：后端 @Max(10) 是硬限制，
               这里再按余票收窄一层，纯为体验（真正的把关在后端原子扣减） -->
          <el-input-number v-model="quantity" :min="1" :max="maxBuyable" />
        </el-form-item>
        <el-form-item label="预计总价">
          <span class="price">¥{{ formatPrice(previewTotal) }}</span>
        </el-form-item>
      </el-form>

      <el-button
        type="primary"
        :loading="orderStore.submitting"
        :disabled="selectedMatchId === null || soldOut"
        @click="onSubmit"
      >
        {{ soldOut ? '已售罄' : '提交订单' }}
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
/* V2：余票标签 —— 正常灰 / 紧张橙 / 售罄红 */
.stock {
  color: #909399;
  font-size: 12px;
}
.stock.warn {
  color: #e6a23c;
  font-weight: 500;
}
.stock.sold-out {
  color: #f56c6c;
}
.no-action {
  color: #c0c4cc;
}
</style>
