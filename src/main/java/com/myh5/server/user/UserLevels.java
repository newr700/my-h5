package com.myh5.server.user;

/**
 * 用户等级登记表 —— 等级是【权限依据】，所以它必须像错误码一样集中登记。
 *
 * ── 为什么要单独一个类，而不是在判断处直接写 userLevel >= 2 ──────────
 * 和 ErrorCodes 完全同一个理由：写成 2 的魔法数字，三个月后没人记得 2 是什么，
 * 更要命的是「等级 2 能发评论」这条规则会散落在 Controller、Service、前端三处。
 * 收敛到这里之后，规则改了只动一个地方，IDE 的「查找引用」还能列出全部使用点。
 *
 * ── 后端是唯一裁判 ─────────────────────────────────────────
 * 前端也会拿这个等级控制「评论框显示不显示」，但那只影响体验：
 * 改 JS 就能绕过。真正的门是 AnalysisService.addComment 里那次判断 ——
 * 前端管好看，后端管对不对，两边做的是同一件事但目的不同。
 */
public final class UserLevels {

    private UserLevels() {
    }

    /** 普通球迷：注册即得，只能看不能发评论 */
    public static final int NORMAL = 1;

    /** 行业专家：可以在权威解析下发表评论（草图上「高用户等级的可以在这里写评论」） */
    public static final int EXPERT = 2;
}
