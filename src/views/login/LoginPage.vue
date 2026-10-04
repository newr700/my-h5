<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'

/**
 * 登录/注册页（PRD-F6/F7）。
 *
 * ── 「前端校验为体验，后端校验为安全」的现场 ──────────────
 * van-form 的 rules 会在提交前拦下空用户名/短密码 —— 这是为了用户体验
 * （不用等一个网络来回才知道密码太短）。但 curl 一条命令就能绕过它，
 * 所以 Java 端 RegisterRequest 上还有一套 @Valid 规则（错误码 1001）——
 * 两套规则是刻意的冗余，不是重复劳动。
 *
 * （技能点：v-model 双向绑定 / 表单校验 / 异步提交防连点）
 */
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const activeTab = ref(0)
const submitting = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '' })

/** 登录成功后回跳：路由守卫把用户原本想去的页面放在 ?redirect= 里带过来 */
function goBack() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/user'
  router.replace(redirect)
}

async function onLogin() {
  submitting.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    showToast('登录成功')
    goBack()
  } catch (e) {
    // 后端会返回 2002「用户名或密码错误」（模糊报错，防枚举）—— 直接展示后端的话
    showToast(e instanceof Error ? e.message : '登录失败')
  } finally {
    submitting.value = false
  }
}

async function onRegister() {
  submitting.value = true
  try {
    await userStore.register(registerForm.username, registerForm.password, registerForm.nickname)
    showToast('注册成功，已自动登录')
    goBack()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="登录" />

    <van-tabs v-model:active="activeTab" shrink>
      <van-tab title="登录">
        <!-- van-form 的 @submit 只在所有 rules 通过后才触发 ——
             校验不通过时提交函数根本不会执行，不用自己写 if -->
        <van-form @submit="onLogin">
          <van-cell-group inset>
            <van-field
              v-model="loginForm.username"
              name="username"
              label="用户名"
              placeholder="4-32 位"
              :rules="[
                { required: true, message: '请输入用户名' },
                { pattern: /^.{4,32}$/, message: '用户名长度需在 4-32 之间' }
              ]"
            />
            <van-field
              v-model="loginForm.password"
              type="password"
              name="password"
              label="密码"
              placeholder="6-32 位"
              :rules="[
                { required: true, message: '请输入密码' },
                { pattern: /^.{6,32}$/, message: '密码长度需在 6-32 之间' }
              ]"
            />
          </van-cell-group>
          <div class="submit-area">
            <van-button type="primary" block round native-type="submit" :loading="submitting">
              登录
            </van-button>
          </div>
        </van-form>
      </van-tab>

      <van-tab title="注册">
        <van-form @submit="onRegister">
          <van-cell-group inset>
            <van-field
              v-model="registerForm.username"
              name="username"
              label="用户名"
              placeholder="4-32 位"
              :rules="[
                { required: true, message: '请输入用户名' },
                { pattern: /^.{4,32}$/, message: '用户名长度需在 4-32 之间' }
              ]"
            />
            <van-field
              v-model="registerForm.password"
              type="password"
              name="password"
              label="密码"
              placeholder="6-32 位"
              :rules="[
                { required: true, message: '请输入密码' },
                { pattern: /^.{6,32}$/, message: '密码长度需在 6-32 之间' }
              ]"
            />
            <van-field
              v-model="registerForm.nickname"
              name="nickname"
              label="昵称"
              placeholder="选填，默认同用户名"
            />
          </van-cell-group>
          <div class="submit-area">
            <van-button type="primary" block round native-type="submit" :loading="submitting">
              注册并登录
            </van-button>
          </div>
        </van-form>
      </van-tab>
    </van-tabs>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}
.submit-area {
  margin: 16px;
}

@media (min-width: 768px) {
  /* 表单在 PC 上不需要占满 900px 内容区 —— 输入框拉太长反而难用 */
  .submit-area {
    max-width: 320px;
    margin: 24px auto;
  }
}
</style>
