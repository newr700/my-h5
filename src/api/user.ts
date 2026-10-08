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
