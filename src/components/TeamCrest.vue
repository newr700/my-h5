<script setup lang="ts">
/**
 * 队徽组件 —— 三张页面都要显示「队徽 + 队名」，所以放在全局共享区。
 *
 * ── 为什么自己画盾牌而不是只用 <img> ──────────────────────────
 * 后端字典里的 logoUrl 目前是空字符串（没有真实版权素材），
 * 如果组件只会 <img>，整站队徽位置就是一片白块。
 * 所以这里做三级降级：有图用图 → 没图用【球队主辅色 + 缩写】画一枚盾牌 →
 * 连主色都没有就用灰色。
 *
 * 这条原则叫「把有图当加分项，把没图当正常情况」：
 * 只在素材齐全时才好看的组件，是不完整的组件。
 *
 * ── 颜色为什么能直接用 ────────────────────────────────────
 * 契约里 colorPrimary / colorSecondary 就是 #RRGGBB，是合法的 CSS 颜色值，
 * 前端不做任何映射表 —— 新增球队只要在字典里填颜色，这里自动生效。
 */
const props = withDefaults(
  defineProps<{
    /** 三字母缩写，如 ARS / MCI；兜底时显示在盾牌上 */
    shortName: string
    colorPrimary?: string
    colorSecondary?: string
    /** 有真实队徽图片时优先显示它；空字符串表示没有 */
    logoUrl?: string
    /** 边长（px） */
    size?: number
    /** 队伍名，用于图片 alt 与悬浮提示 */
    teamName?: string
  }>(),
  {
    colorPrimary: '#909399',
    colorSecondary: '#C0C4CC',
    logoUrl: '',
    size: 36,
    teamName: ''
  }
)
</script>

<template>
  <span
    class="crest"
    :style="{ width: size + 'px', height: size + 'px' }"
    :title="teamName || shortName"
  >
    <!-- ① 有图走图片 -->
    <img v-if="logoUrl" :src="logoUrl" :alt="teamName || shortName" class="crest-img" />
    <!-- ② 没图：用球队配色画盾牌 + 缩写 -->
    <svg v-else class="crest-svg" viewBox="0 0 24 24" role="img" :aria-label="teamName || shortName">
      <!-- 盾形外轮廓：上半部平直，下半部收成尖角，是球会徽章的经典轮廓 -->
      <path
        d="M12 2.2 L20.4 5.2 V11.4 C20.4 16.2 16.8 20.3 12 21.8 C7.2 20.3 3.6 16.2 3.6 11.4 V5.2 Z"
        :fill="props.colorPrimary"
        :stroke="props.colorSecondary"
        stroke-width="1.1"
      />
      <!-- 内圈用辅色再描一层，让单色队也能看出两支队伍的区别 -->
      <path
        d="M12 4.4 L18.4 6.6 V11.4 C18.4 15.2 15.6 18.4 12 19.6 C8.4 18.4 5.6 15.2 5.6 11.4 V6.6 Z"
        fill="none"
        :stroke="props.colorSecondary"
        stroke-width="0.7"
        opacity="0.75"
      />
      <!-- 缩写：短名可能超长（如 TBD 三字母），用 textLength 兜住不溢出 -->
      <text
        x="12"
        y="13.6"
        text-anchor="middle"
        font-size="6.4"
        font-weight="700"
        fill="#ffffff"
        font-family="Arial, Helvetica, sans-serif"
      >
        {{ shortName.slice(0, 3).toUpperCase() }}
      </text>
    </svg>
  </span>
</template>

<style scoped>
.crest {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  vertical-align: middle;
  line-height: 0;
}
.crest-img,
.crest-svg {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 4px;
}
</style>
