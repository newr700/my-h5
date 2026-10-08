<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * 登录/注册页（PRD-F6/F7）。
 *
 * ── 「前端校验为体验，后端校验为安全」的现场 ──────────────
 * el-form 的 :rules 会在提交前拦下空用户名/短密码 —— 这是为了用户体验
 * （不用等一个网络来回才知道密码太短）。但 curl 一条命令就能绕过它，
 * 所以 Java 端 RegisterRequest 上还有一套 @Valid 规则（错误码 1001）——
 * 两套规则是刻意的冗余，不是重复劳动。
 *
 * （技能点：v-model 双向绑定 / 表单校验 / 异步提交防连点 / el-form 的 validate 守卫）
 */
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const activeTab = ref('login')
const submitting = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '' })

// el-form 的 rules：required 拦空、pattern 拦格式；trigger 用 blur（失焦即校验）
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^.{4,32}$/, message: '用户名长度需在 4-32 之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^.{6,32}$/, message: '密码长度需在 6-32 之间', trigger: 'blur' }
  ]
}
const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^.{4,32}$/, message: '用户名长度需在 4-32 之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^.{6,32}$/, message: '密码长度需在 6-32 之间', trigger: 'blur' }
  ],
  // 昵称选填：不写 required，留空时后端默认同用户名
  nickname: [{ max: 32, message: '昵称过长', trigger: 'blur' }]
}

// 两个表单各自的 ref —— el-form 的 validate() 是 Promise，
// 校验不过会 reject，所以我们 try/catch 一把，失败的提交直接 return。
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

/** 登录成功后回跳：路由守卫把用户原本想去的页面放在 ?redirect= 里带过来 */
function goBack() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/user'
  router.replace(redirect)
}

async function onLogin() {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate() // 校验不过这里会抛，进 catch 直接 return
  } catch {
    return
  }
  submitting.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')
    goBack()
  } catch (e) {
    // 后端会返回 2002「用户名或密码错误」（模糊报错，防枚举）—— 直接展示后端的话
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    submitting.value = false
  }
}

async function onRegister() {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await userStore.register(registerForm.username, registerForm.password, registerForm.nickname)
    ElMessage.success('注册成功，已自动登录')
    goBack()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <el-card class="login-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <!--
            el-form 的提交守卫思路：点按钮才触发 onLogin，函数内部先 validate()。
            和 Vant 的 @submit 不同，Element Plus 不自动拦截——校验是我们主动调用的，
            更显式，也更好理解「为什么校验不过就不会发请求」。
          -->
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-width="72px"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="4-32 位" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                placeholder="6-32 位"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="onLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-width="72px"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="4-32 位" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                placeholder="6-32 位"
              />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="选填，默认同用户名" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="onRegister">
                注册并登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 16px;
}
.login-card {
  width: 100%;
  max-width: 420px;
}
</style>
