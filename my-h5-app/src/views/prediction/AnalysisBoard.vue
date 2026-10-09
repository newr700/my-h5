<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import TeamCrest from '@/components/TeamCrest.vue'
import { useAnalysisStore } from '@/stores/prediction'
import { useUserStore } from '@/stores/user'

/**
 * 页面2：权威解析（草图对应的第一张）
 *
 * 三件事：
 *   ① 中/英切换 —— 专家姓名和球队名都有两套说法，切换只改展示，不重新请求接口；
 *   ② 球队配色标记 —— 每位专家支持哪支队，连线/标签就用该队的颜色（颜色由字典下发）；
 *   ③ 评论 —— 都看得到，但【只有行业专家能写】。
 *
 * ── 关于页面边界 ──────────────────────────────────────────
 * 这个页面不自己判断「能不能发」的算法，只问 userStore.isExpert。
 * 真正的校验在后端：即使有人改 JS 把输入框显示出来，
 * 请求发过去也会被后端的等级守卫拦下并返回 6002。
 */
const analysisStore = useAnalysisStore()
const userStore = useUserStore()
const router = useRouter()

/** 展示语言：切换只影响「用哪个字段显示」，数据是同一份 */
const lang = ref<'zh' | 'en'>('zh')

/** 每条解析的评论展开状态（默认收起：首屏不该背着所有评论） */
const expanded = ref<Record<number, boolean>>({})
/** 每条解析正在提交的评论 */
const drafts = ref<Record<number, string>>({})
const submitting = ref<Record<number, boolean>>({})

/** 专家姓名按当前语言取：中文优先给中文名，英文化时给本名 */
/** 专家姓名按当前语言取：中文优先给中文名，英文化时给本名；文档无英文名则回退中文 */
const expertName = (zh: string, en: string) => (lang.value === 'zh' ? zh : (en || zh))
const teamLabel = (zh: string, en: string) => (lang.value === 'zh' ? zh : en)

const analyses = computed(() => analysisStore.analyses)

/**
 * 展开/收起评论区。
 * 评论在进页面时已经一次性拉全了，所以这里只切换本地状态、不发请求 ——
 * 展开即出内容，不会「先空一下再突然填满」。
 */
function toggleComments(id: number) {
  expanded.value = { ...expanded.value, [id]: !expanded.value[id] }
}

/** 某条解析的评论拉取失败后的重试（只重拉这一条） */
async function retryComments(id: number) {
  try {
    await analysisStore.loadComments(id)
  } catch {
    ElMessage.error('评论加载失败，请稍后重试')
  }
}

async function submitComment(id: number) {
  const content = (drafts.value[id] ?? '').trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  // 后端的长度上限是 500（@Size(max=500)），这里提前拦一下，
  // 免得让用户打完字才被告知太长 —— 前端校验为体验，后端校验为安全
  if (content.length > 500) {
    ElMessage.warning(`评论最多 500 字，当前 ${content.length} 字`)
    return
  }

  submitting.value = { ...submitting.value, [id]: true }
  try {
    await analysisStore.addComment(id, content)
    drafts.value = { ...drafts.value, [id]: '' }
    ElMessage.success('评论已发布')
    // 刚发的评论可能不在原来的缓存里（首次展开即发），确保展开以看到自己的发言
    expanded.value = { ...expanded.value, [id]: true }
  } catch (err) {
    // 6002（等级不足）等后端错误会带明确原因，原样提示比笼统说失败有用
    ElMessage.error(err instanceof Error ? err.message : '评论发布失败')
  } finally {
    submitting.value = { ...submitting.value, [id]: false }
  }
}

async function applyExpert() {
  try {
    await userStore.applyExpert()
    ElMessage.success('已成为行业专家，可以发表评论了')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '升级失败')
  }
}

onMounted(async () => {
  try {
    await analysisStore.loadAnalyses()
  } catch {
    // 首屏错误已写进 store.error，页面会渲染错误态，这里不需要额外处理
    return
  }
  // 解析列表到手后，一次性把所有评论拉全：
  // 之后点开任意一条都是本地切换，展开即出内容，不会再有「等一下才冒出来」的闪动
  analysisStore.loadAllComments(analysisStore.analyses.map((a) => a.id)).catch(() => {
    /* loadAllComments 内部用 allSettled，单条失败已记进 commentsError */
  })
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="title">权威解析</h2>
        <p class="subtitle">资深解说员与评论员眼中的夺冠格局</p>
      </div>
      <!-- 语言切换：只切换展示用语，不触发新的网络请求 -->
      <el-radio-group v-model="lang" size="small">
        <el-radio-button value="zh">中文</el-radio-button>
        <el-radio-button value="en">EN</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 加载中：骨架屏（首次进入，一条数据都还没有） -->
    <div v-if="analysisStore.loading" class="grid">
      <el-skeleton v-for="i in 4" :key="i" animated class="card-skeleton">
        <template #template>
          <el-skeleton-item variant="circle" style="width: 44px; height: 44px" />
          <el-skeleton-item variant="p" style="width: 60%; margin-top: 12px" />
          <el-skeleton-item variant="text" style="margin-top: 12px" />
          <el-skeleton-item variant="text" style="width: 80%" />
        </template>
      </el-skeleton>
    </div>

    <!-- 加载失败且没有任何数据：错误必须占满屏幕，而不是拿假数据糊过去 -->
    <el-result
      v-else-if="analysisStore.error"
      icon="error"
      title="解析加载失败"
      :sub-title="analysisStore.error"
    >
      <template #extra>
        <el-button type="primary" @click="analysisStore.loadAnalyses()">重试</el-button>
      </template>
    </el-result>

    <div v-else class="grid">
      <el-card
        v-for="item in analyses"
        :key="item.id"
        class="expert-card"
        shadow="hover"
        :style="{ '--team-color': item.colorPrimary }"
      >
        <template #header>
          <div class="expert-head">
            <!-- 头像：没有图片时用姓名首字母占位 -->
            <el-avatar :size="42" :src="item.avatarUrl || undefined">
              {{ expertName(item.nameCn, item.nameEn).charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="expert-meta">
              <div class="expert-name">{{ expertName(item.nameCn, item.nameEn) }}</div>
              <div v-if="item.title" class="expert-title">{{ item.title }}</div>
              <div class="expert-sub">
                {{ lang === 'zh' ? '支持' : 'Backs' }}
                <!-- 球队名 + 队徽：队徽颜色取自该队字典里的主/辅色 -->
                <TeamCrest
                  :short-name="item.shortName"
                  :color-primary="item.colorPrimary"
                  :color-secondary="item.colorSecondary"
                  :logo-url="item.logoUrl"
                  :team-name="teamLabel(item.teamName, item.teamNameEn)"
                  :size="20"
                />
                <strong class="team">{{ teamLabel(item.teamName, item.teamNameEn) }}</strong>
              </div>
            </div>
          </div>
        </template>

        <p class="reason">{{ item.reason }}</p>

        <div class="card-footer">
          <el-button text type="primary" @click="toggleComments(item.id)">
            {{ lang === 'zh' ? '评论' : 'Comments' }}（{{ item.commentCount }}）
            <el-icon class="caret" :class="{ open: expanded[item.id] }"><ArrowDown /></el-icon>
          </el-button>
        </div>

        <!-- 评论区：收起时不渲染（长列表不必白占 DOM）。展开即出内容 ——
             评论在进页面时已一次性拉全，所以这里不再发请求。
             外层 comments-wrap 是容器，.comments 是横向排开的评论列表（卡片左右滚动），
             输入框/升级入口在列表下方占满整行 -->
        <div v-if="expanded[item.id]" class="comments-wrap">
          <!-- 这批评论拉取失败：如实说 + 给重试，不用「还没有人发表观点」糊过去 -->
          <div v-if="analysisStore.commentsError[item.id]" class="comments-error">
            <span>评论加载失败：{{ analysisStore.commentsError[item.id] }}</span>
            <el-button size="small" type="primary" text @click="retryComments(item.id)">
              重试
            </el-button>
          </div>
          <template v-else>
            <div class="comments">
              <!-- 批量拉取还没回来时显示「加载中」：
                   否则会先闪一句「还没有人发表观点」再变成列表，像抽了一下 -->
              <div v-if="!analysisStore.commentsReady" class="empty-comment">
                {{ lang === 'zh' ? '评论加载中…' : 'Loading…' }}
              </div>
              <div v-else-if="analysisStore.commentsOf(item.id).length === 0" class="empty-comment">
                {{ lang === 'zh' ? '还没有人发表观点' : 'No comments yet' }}
              </div>
              <div v-for="c in analysisStore.commentsOf(item.id)" :key="c.id" class="comment">
                <el-avatar :size="28">{{ c.nickname.charAt(0).toUpperCase() }}</el-avatar>
                <div class="comment-body">
                  <div class="comment-head">
                    <span class="cname">{{ c.nickname }}</span>
                    <!-- 等级标签用发布当时的快照，不是用户现在的等级 -->
                    <el-tag v-if="c.userLevel >= 2" size="small" type="warning" effect="light">
                      行业专家
                    </el-tag>
                    <span class="ctime">{{ c.createdAt }}</span>
                  </div>
                  <div class="ctext">{{ c.content }}</div>
                </div>
              </div>
            </div>

            <!-- 只有行业专家才看到输入框；其他人看到的是升级入口 -->
            <div v-if="userStore.isExpert" class="compose">
              <el-input
                v-model="drafts[item.id]"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                placeholder="写下你的观点…"
              />
              <el-button
                type="primary"
                size="small"
                class="send"
                :loading="submitting[item.id]"
                @click="submitComment(item.id)"
              >
                发表
              </el-button>
            </div>
            <div v-else class="locked">
              <span>{{
                lang === 'zh'
                  ? '仅行业专家（Lv.2）可在此发表评论'
                  : 'Industry experts (Lv.2) only'
              }}</span>
              <!-- 未登录 → 先引导登录（applyExpert 需要 token，没登录点上去只会拿 401）；
                   已登录但不是专家 → 引导申请升级 -->
              <el-button
                v-if="userStore.isLoggedIn"
                size="small"
                type="primary"
                plain
                @click="applyExpert"
              >
                {{ lang === 'zh' ? '申请成为专家' : 'Apply' }}
              </el-button>
              <el-button v-else size="small" type="primary" plain @click="router.push('/login')">
                {{ lang === 'zh' ? '登录后可申请' : 'Login' }}
              </el-button>
            </div>
          </template>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* 建立堆叠上下文：让下方 ::before 那层背景（z-index:-1）
   正好压在 #app 的白底之上、页面内容之下 */
.page {
  position: relative;
  z-index: 0;

  /* 白色区域统一降透明度：Element Plus 的组件内部都读这些变量，
     改这一处，卡片/输入框/评论区会一起变透 */
  --el-card-bg-color: rgba(255, 255, 255, 0.78);
  --el-fill-color-blank: rgba(255, 255, 255, 0.72);
  --el-fill-color-lighter: rgba(255, 255, 255, 0.6);
}

/* ── 主题背景（权威解析配色：蓝紫）────────────────────────────
 * 固定定位的伪元素铺满视口当壁纸：不参与布局（原有间距不动）、
 * 锚定视口（页面再长也不会把图拉伸变形）、z-index:-1（永远在内容下面）。
 * 上面叠一层白色半透明遮罩，压低背景对比度，保证卡片上的文字读得清。 */
.page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.42)),
    url('../../assets/backgrounds/analysis.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.title {
  margin: 0;
  font-size: 20px;
}
.subtitle {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
/* 权威解析：每块解析横向占满（单列、撑满整行），
   不再多列并排 —— 让专家观点有更宽的阅读宽度 */
.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}
.card-skeleton {
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  /* 骨架屏也跟正式卡片一样半透明，加载完切换时不会「白块突然变透」 */
  background: rgba(255, 255, 255, 0.72);
}
/* 卡片左侧用球队主色描一道竖线 —— 草图批注「专家可以选择颜色来标记（用球队颜色）」 */
.expert-card {
  border-left: 4px solid var(--team-color, var(--el-color-primary));
}
.expert-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.expert-name {
  font-size: 15px;
  font-weight: 600;
}
.expert-title {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.expert-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.team {
  color: var(--team-color, var(--el-color-primary));
}
.reason {
  margin: 0 0 8px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.card-footer {
  display: flex;
  justify-content: flex-end;
}
.caret {
  transition: transform 0.2s;
  margin-left: 4px;
}
.caret.open {
  transform: rotate(180deg);
}
.comments-wrap {
  margin-top: 12px;
  border-top: 1px dashed var(--el-border-color-lighter);
  padding-top: 12px;
}
/* 横向展开：评论以卡片形式横向排开，超出视口可左右滚动。
   每条评论是一张固定宽度的卡片（头像 + 内容上下排布），整体向右延伸。 */
.comments {
  display: flex;
  flex-direction: row;
  align-items: stretch;
  gap: 12px;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 6px;
}
.empty-comment {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  padding: 8px 0;
  flex: 0 0 auto;
}
.comment {
  flex: 0 0 300px;
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}
.comment-body {
  min-width: 0;
  flex: 1;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.cname {
  font-weight: 600;
  font-size: 13px;
}
.ctime {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.ctext {
  margin-top: 2px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.compose {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
/* 评论拉取失败：和「仅专家可评」同一套浅色提示条，右侧是重试按钮 */
.comments-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  padding: 8px 10px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.locked {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  padding: 8px 10px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
