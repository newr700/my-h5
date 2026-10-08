package com.myh5.server.prediction.vo;

/**
 * 明星球员（VO）—— 草图上「可以跟一两位明星球员照片」。
 *
 * photoUrl 没图时给 ''，前端用「球衣号 + 姓名首字」的圆牌兜底 ——
 * 与队徽/头像同一套降级策略：把「有图」当加分项，把「没图」当正常情况。
 * 一个页面只有在两种情况下都好看，才算真的做完了。
 */
public record StarPlayerVo(
        String playerName,
        String position,
        int jerseyNumber,
        String photoUrl
) {
}
