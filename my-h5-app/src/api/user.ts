import { post } from './request'

/**
 * 用户模块接口（对应 Java 端 UserController 的头像上传）。
 *
 * 为什么不直接用 el-upload 自带的 XHR：
 * el-upload 默认走自己的 XMLHttpRequest，不会带我们封装的 token 拦截器，
 * 也不会拆 {code,message,data} 外壳 —— 等于把项目统一的鉴权/错误处理绕开了。
 * 所以这里用自定义 http-request，复用 api/request.ts 的 post（自动带 token + 拆外壳 + 统一报错）。
 */
export interface UploadAvatarResult {
  /** 后端返回的头像相对路径，如 /uploads/12_xxx.png */
  avatarUrl: string
}

/**
 * 上传头像：入参是浏览器 File，内部包成 FormData 发 multipart/form-data。
 * 后端 Result.ok 的 data 就是一个字符串（avatarUrl），所以这里直接收字符串。
 */
export async function uploadAvatar(file: File): Promise<UploadAvatarResult> {
  const form = new FormData()
  form.append('file', file)
  // post 已替我们做完：带 token、校验 code、拆外壳、失败抛错
  const avatarUrl = await post<string>('/user/avatar', form)
  return { avatarUrl }
}

/**
 * 申请成为行业专家（V7 新增，对应 Java UserController 的 /user/expert-apply）。
 *
 * ── 为什么返回 void 而不是返回新等级 ─────────────────────
 * 让 store 去重新拉一次 /user/profile，而不是相信这次返回的那个数字：
 * 用户等级只有一个权威来源。本地顺手改一下看似省事，
 * 但刷新页面就会露馅 ——「写完假装成功」是最难查的一类 bug。
 *
 * ── 与真实业务的边界 ────────────────────────────────────
 * 本项目是【模拟审核】：调用一次即通过，方便演示权限流转。
 * 真实系统里这一步应当只提交申请、由管理员审批，
 * 那时返回体应该是「申请状态」而不是直接改等级 —— 这里的调用方不用改，
 * 因为是否真的升级了，一律以重新拉取的资料为准。
 */
export async function applyExpert(): Promise<void> {
  await post<unknown>('/user/expert-apply')
}
