-- ============================================================
-- V7 英超预测内容域：球队资料 / 六维评分 / 明星球员 / 权威解析 / 评论 / 历届战绩
-- ------------------------------------------------------------
-- 【这一版解决什么】
-- 前端新增三个页面（草图「页面2 权威解析 / 页面4 AI 预测 / 页面5 历史回顾」），
-- 它们的共同点是「都围绕球队展开」：权威解析要按球队上色、AI 预测要按球队画雷达、
-- 历史回顾要显示冠亚季军队徽。所以先抽一张球队资料表，让三个页面共用一份颜色/缩写，
-- 而不是在每张业务表里各抄一份颜色 —— 颜色码改一次要改三个地方，是必然出错的写法。
--
-- 【为什么不建外键】
-- 沿用本项目既有约定（V1 的 match_order 只存 match_id 不建外键）：
-- 这套表是「展示型内容」，与球队资料是弱依赖 —— 资料表少一行不该让业务表插不进去。
-- 一致性由 Service 层 + 迁移脚本保证，不靠数据库约束。
--
-- 【兼容性】同 V1~V6：只用 MySQL 8 与 H2（MySQL 模式）都支持的写法，
--   因为 src/test 会用 H2 内存库执行同一份迁移脚本（见 src/test/resources/application.yml）。
-- ============================================================


-- ── 1. 球队资料字典 ──────────────────────────────────────────
-- 草图上那句「颜色有对应码，和资料一起出」在这里落地：
-- 颜色不是前端写死的常量，而是随球队资料一起下发的数据。
-- logo_url 预留给真队徽；现在为空，前端用「双色盾牌 + 三字母缩写」兜底
-- （与积分榜 logoUrl 的降级思路一致：坑位稳定，有无图片都不影响布局）。
CREATE TABLE IF NOT EXISTS football_team (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    team_name       VARCHAR(50)  NOT NULL COMMENT '中文队名，作为业务侧引用键',
    team_name_en    VARCHAR(64)  NOT NULL COMMENT '英文队名，权威解析的中/英切换用',
    short_name      VARCHAR(8)   NOT NULL COMMENT '三字母缩写，兜底队徽上显示的文字',
    color_primary   VARCHAR(16)  NOT NULL COMMENT '球队主色 #RRGGBB',
    color_secondary VARCHAR(16)  NOT NULL COMMENT '球队辅色，兜底队徽的斜条与描边',
    logo_url        VARCHAR(512) DEFAULT NULL COMMENT '真队徽图片地址；NULL 表示暂无，前端走兜底',
    CONSTRAINT uk_football_team_name UNIQUE (team_name)
);


-- ── 2. AI 预测：球队六维评分 ─────────────────────────────────
-- 【关键设计：表里没有「夺冠概率」这一列】
-- 概率是【算出来的】而不是【存下来的】，理由和积分榜不存 rank 列完全一致：
-- 存了就要在每次调整评分时同步维护它，两处真相早晚打架。
-- 六维分数才是原始事实，概率由 PredictionScoring 里的加权模型现算 ——
-- 权重改了，全表概率自动跟着变，不用写一条 UPDATE 去回填。
--
-- 六维分别对应草图上六边形的六个顶点（各 0~100）：
--   history   历史夺冠次数
--   star      明星球员
--   home_away 主客优势
--   tactic    战术分析
--   matchup   对位优势
--   squad     阵容实力
CREATE TABLE IF NOT EXISTS team_prediction (
    id               BIGINT      AUTO_INCREMENT PRIMARY KEY,
    team_name        VARCHAR(50) NOT NULL,
    history_score    INT         NOT NULL DEFAULT 50 COMMENT '历史夺冠次数 0~100',
    star_score       INT         NOT NULL DEFAULT 50 COMMENT '明星球员 0~100',
    home_away_score  INT         NOT NULL DEFAULT 50 COMMENT '主客优势 0~100',
    tactic_score     INT         NOT NULL DEFAULT 50 COMMENT '战术分析 0~100',
    matchup_score    INT         NOT NULL DEFAULT 50 COMMENT '对位优势 0~100',
    squad_score      INT         NOT NULL DEFAULT 50 COMMENT '阵容实力 0~100',
    CONSTRAINT uk_team_prediction_team UNIQUE (team_name)
);


-- ── 3. 明星球员（草图上「可以跟一两位明星球员照片」）──────────────
-- 只给头部球队配 1~2 名，其余球队留空 —— 页面对空列表必须能优雅降级
-- （不能因为是空的就塌掉一块布局，这是列表页的基本修养）。
-- photo_url 同样预留：NULL 时前端用「球衣号 + 首字」的圆牌兜底。
CREATE TABLE IF NOT EXISTS team_star_player (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    team_name     VARCHAR(50)  NOT NULL,
    player_name   VARCHAR(64)  NOT NULL,
    position      VARCHAR(16)  NOT NULL COMMENT '位置，如 前锋 / 前腰 / 中卫',
    jersey_number INT          NOT NULL DEFAULT 0 COMMENT '球衣号',
    photo_url     VARCHAR(512) DEFAULT NULL COMMENT '球员照片；NULL 走首字兜底',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '同队内展示顺序'
);
CREATE INDEX idx_team_star_player_team ON team_star_player (team_name);


-- ── 4. 权威解析（专家观点卡片）─────────────────────────────────
-- 草图上批注「可选用中/英」——所以中英文名两列都存，前端切换时不用请求第二遍。
-- 头像（草图上「抓头像」）同样走「有图用图、无图用首字母」的既有约定。
CREATE TABLE IF NOT EXISTS expert_analysis (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    expert_name_en VARCHAR(64)  NOT NULL COMMENT '英文名，如 Martin Tyler',
    expert_name_cn VARCHAR(32)  NOT NULL COMMENT '中文名，如 马丁·泰勒',
    team_name      VARCHAR(50)  NOT NULL COMMENT '该专家支持的球队，决定卡片配色',
    reason         VARCHAR(500) NOT NULL COMMENT '支持理由，卡片正文',
    avatar_url     VARCHAR(512) DEFAULT NULL COMMENT '头像；NULL 走首字兜底',
    sort_order     INT          NOT NULL DEFAULT 0 COMMENT '展示顺序'
);


-- ── 5. 权威解析的评论（本期做成可用功能）──────────────────────
-- 【为什么把 nickname / user_level 存成快照，而不是只存 user_id 再 JOIN】
-- 两个理由，一个是性能一个是业务：
--   ① 评论列表是本页最热的读路径，每次读都 JOIN app_user 只是为了拿一个昵称；
--   ② 更重要的是「等级快照」的业务含义：评论能发出来，是因为【发表当时】
--      这个人是行业专家。事后他被降级，历史评论不该跟着变成「无权发言」的怪状态；
--      反过来他升到专家，也不能让几个月前发不出的评论忽然成立。
--      这和订单里存「下单时的单价快照」是同一个道理：记录事实发生那一刻的值。
-- user_id 允许为 NULL：种子数据里的演示评论不对应任何真实账号。
CREATE TABLE IF NOT EXISTS expert_comment (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    analysis_id BIGINT       NOT NULL COMMENT '评论挂在哪条权威解析下',
    user_id     BIGINT       DEFAULT NULL COMMENT '发表者 id；NULL = 演示数据',
    nickname    VARCHAR(32)  NOT NULL COMMENT '发布时的昵称快照',
    user_level  TINYINT      NOT NULL DEFAULT 1 COMMENT '发布时的用户等级快照',
    content     VARCHAR(500) NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 查询永远带 analysis_id 条件（点开某条解析才拉它的评论），索引必须有
CREATE INDEX idx_expert_comment_analysis ON expert_comment (analysis_id);


-- ── 6. 历届战绩（页面5 上方的表格）────────────────────────────
-- edition（届数）和 season_year 都存：届数是「第几届」的口径（英格兰顶级联赛
-- 从 1992-93 赛季算第 1 届），season_year 是起始年份（2000 表示 2000-01 赛季）。
-- 两个口径都要给用户看，所以都存；而不是让前端拿年份去减 1991 反推届数
-- （把口径计算放前端，改了规则就要发前端版本）。
CREATE TABLE IF NOT EXISTS season_history (
    id              BIGINT      AUTO_INCREMENT PRIMARY KEY,
    edition         INT         NOT NULL COMMENT '届数，1992-93 赛季为第 1 届',
    season_year     INT         NOT NULL COMMENT '赛季起始年份，2000 表示 2000-01 赛季',
    champion_team   VARCHAR(50) NOT NULL COMMENT '冠军',
    runner_up_team  VARCHAR(50) NOT NULL COMMENT '亚军',
    third_team      VARCHAR(50) NOT NULL COMMENT '季军',
    CONSTRAINT uk_season_history_edition UNIQUE (edition)
);
CREATE INDEX idx_season_history_year ON season_history (season_year);


-- ── 7. 用户等级（评论权限的依据）──────────────────────────────
-- 草图上「高用户等级的可以在这里写评论（行业专家等级）」需要一个可判定的等级字段。
--   1 = 普通球迷（默认，注册即得）
--   2 = 行业专家（可发表权威解析评论）
-- 默认值 1 保证存量用户升级到本版本后行为不变，且不能发言 ——
-- 「新功能默认不给权限」比「默认给全部权限」安全得多。
--
-- 【列名为什么叫 user_level 而不是 level】
-- level 在 SQL 标准里是游标层级的保留词（MySQL 里是非保留、H2 里各版本口径不一），
-- 列名跟保留词套近乎只会换来一堆莫名其妙的语法报错 —— 绕开它，代价是多打五个字符。
ALTER TABLE app_user ADD COLUMN user_level TINYINT NOT NULL DEFAULT 1
    COMMENT '用户等级：1=普通球迷 2=行业专家（可发评论）';
