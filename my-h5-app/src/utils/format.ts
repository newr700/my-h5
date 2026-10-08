/** 纯工具函数：不依赖 Vue、不发起请求，输入确定输出就确定 */

/** 分转元，保留两位小数 */
export function formatPrice(fen: number): string {
  return (fen / 100).toFixed(2)
}
