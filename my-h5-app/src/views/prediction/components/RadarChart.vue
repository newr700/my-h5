<script setup lang="ts">
import { computed } from 'vue'
import type { DimScore } from '@/types/api'

/**
 * 六维雷达图 —— 手写 SVG，没有引 echarts / chart.js。
 *
 * ── 为什么不引图表库 ──────────────────────────────────────
 * 需求是「六个固定维度的正多边形 + 一组数值」，形状永远不会变复杂。
 * 为此装一个几百 KB 的图表库，换来的是打包体积和一个看不懂的配置对象。
 * 自己画大约 60 行，改动完全可控 —— 工具该按问题规模挑，而不是默认上最大的那把锤子。
 *
 * ── 「数字可调」是什么意思（对应草图批注）─────────────────
 * showValue 控制是否在顶点旁写出具体分数：
 * 看图时数字会干扰形状的整体感觉，需要核对时又没有数字不行，
 * 所以做成开关交给用户，而不是替用户决定。
 */
const props = withDefaults(
  defineProps<{
    /** 六个维度，顺序即雷达图顶点的顺时针顺序（后端保证） */
    dims: DimScore[]
    colorPrimary?: string
    /** 是否在每个顶点旁显示数值 */
    showValue?: boolean
  }>(),
  { colorPrimary: '#409eff', showValue: true }
)

/** 满分线：六维评分统一是 0~100，所以雷达图的边界就是 100 */
const MAX_SCORE = 100
/** 画几圈网格。4 圈 = 25/50/75/100 四档，够读又不至于糊成一团 */
const RING_COUNT = 4
const CX = 130
const CY = 112
const R = 78

/** 每个维度对应的角度：从正上方开始，顺时针均匀铺开 */
function angleOf(index: number, total: number): number {
  return (Math.PI * 2 * index) / total - Math.PI / 2
}

/** 把「第 index 个维度、距圆心 r」换算成画布坐标 */
function pointAt(index: number, total: number, r: number): [number, number] {
  const a = angleOf(index, total)
  return [CX + Math.cos(a) * r, CY + Math.sin(a) * r]
}

const total = computed(() => props.dims.length)

/** 网格：从外到内 RING_COUNT 圈正多边形 */
const rings = computed(() =>
  Array.from({ length: RING_COUNT }, (_, k) => {
    const r = R * ((k + 1) / RING_COUNT)
    return props.dims
      .map((_, i) => pointAt(i, total.value, r).map((v) => v.toFixed(1)).join(','))
      .join(' ')
  })
)

/** 从圆心射向各顶点的轴线 */
const axes = computed(() =>
  props.dims.map((_, i) => {
    const [x, y] = pointAt(i, total.value, R)
    return { x: x.toFixed(1), y: y.toFixed(1) }
  })
)

/** 数据多边形：分数先夹到 0~100，脏数据也不会画出界 */
const dataPolygon = computed(() => {
  if (props.dims.length === 0) return ''
  return props.dims
    .map((d, i) => {
      const ratio = Math.max(0, Math.min(1, d.score / MAX_SCORE))
      return pointAt(i, total.value, R * ratio).map((v) => v.toFixed(1)).join(',')
    })
    .join(' ')
})

/** 每个数据点，用来画顶点小圆 */
const dots = computed(() =>
  props.dims.map((d, i) => {
    const ratio = Math.max(0, Math.min(1, d.score / MAX_SCORE))
    const [x, y] = pointAt(i, total.value, R * ratio)
    return { x: x.toFixed(1), y: y.toFixed(1), score: d.score }
  })
)

/** 顶点标签（维度中文名 + 可选数值） */
const labels = computed(() =>
  props.dims.map((d, i) => {
    const [x, y] = pointAt(i, total.value, R + 17)
    // 靠左侧的点右对齐、靠右侧的点左对齐，标签才不会压到图形上
    const anchor = x > CX + 2 ? 'start' : x < CX - 2 ? 'end' : 'middle'
    return { x: x.toFixed(1), y: (y + 3).toFixed(1), anchor, label: d.label, score: d.score }
  })
)
</script>

<template>
  <svg class="radar" viewBox="0 0 260 230" role="img" aria-label="六维能力雷达图">
    <!-- 网格圈：最外圈是满分 100，往内每圈减 25 -->
    <polygon
      v-for="(r, i) in rings"
      :key="'ring' + i"
      :points="r"
      fill="none"
      stroke="var(--radar-grid, #dcdfe6)"
      :stroke-width="i === rings.length - 1 ? 1.2 : 0.8"
    />

    <!-- 轴线 -->
    <line
      v-for="(a, i) in axes"
      :key="'axis' + i"
      :x1="CX"
      :y1="CY"
      :x2="a.x"
      :y2="a.y"
      stroke="var(--radar-grid, #dcdfe6)"
      stroke-width="0.8"
    />

    <!-- 数据区域：填半透明主色 + 描边，哪一维是短板一眼就能看出来 -->
    <polygon
      :points="dataPolygon"
      :fill="props.colorPrimary"
      fill-opacity="0.25"
      :stroke="props.colorPrimary"
      stroke-width="1.8"
      stroke-linejoin="round"
    />

    <!-- 顶点圆点 -->
    <circle
      v-for="(d, i) in dots"
      :key="'dot' + i"
      :cx="d.x"
      :cy="d.y"
      r="3"
      :fill="props.colorPrimary"
    />

    <!-- 维度名 + 数值 -->
    <g v-for="(l, i) in labels" :key="'label' + i">
      <text
        :x="l.x"
        :y="l.y"
        :text-anchor="l.anchor"
        font-size="10.5"
        fill="var(--radar-text, #606266)"
      >
        {{ l.label }}
      </text>
      <!-- 数值换行显示在维度名下方，避免和名字挤在一行 -->
      <text
        v-if="showValue"
        :x="l.x"
        :y="Number(l.y) + 12"
        :text-anchor="l.anchor"
        font-size="10.5"
        font-weight="700"
        :fill="props.colorPrimary"
      >
        {{ l.score }}
      </text>
    </g>
  </svg>
</template>

<style scoped>
.radar {
  width: 100%;
  height: auto;
  max-width: 260px;
  display: block;
  margin: 0 auto;
}
</style>
