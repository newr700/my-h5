-- ============================================================
-- V1 初始化表结构
-- ------------------------------------------------------------
-- 【教学说明】这份文件就是工程手册第 7 章「数据库变更纪律」的执行方式：
--   表结构不靠手工连库创建，而是写成版本化脚本，由 Flyway 在启动时执行。
--   好处：任何一台新机器（同事的、CI 的、线上的）启动应用后，
--   得到的表结构一字不差 —— 「在我机器上是好的」这句话从此失效。
--
-- 【兼容性说明】这份 SQL 故意只用 MySQL 8 和 H2（MySQL 兼容模式）都支持的
--   标准写法（不写 ENGINE=InnoDB / DEFAULT CHARSET 这类 MySQL 专属子句），
--   因为单元测试会用 H2 内存库执行同一份脚本（见 src/test/resources）。
--   字符集由建库时指定的 utf8mb4 继承，不需要在每个表上重复声明。
-- ============================================================

-- 积分榜：注意表里没有 rank 列。
-- 名次是「计算结果」不是「存储数据」—— 存了就要在每次比分变化时维护它，
-- 两处真相必然打架。由查询时 ORDER BY 生成（PRD 裁定：rank 由后端生成）。
CREATE TABLE IF NOT EXISTS team_standing (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_name     VARCHAR(50) NOT NULL,
    played        INT NOT NULL DEFAULT 0 COMMENT '已赛轮次',
    win           INT NOT NULL DEFAULT 0,
    draw          INT NOT NULL DEFAULT 0,
    lose          INT NOT NULL DEFAULT 0,
    goals_for     INT NOT NULL DEFAULT 0 COMMENT '进球数',
    goals_against INT NOT NULL DEFAULT 0 COMMENT '失球数',
    points        INT NOT NULL DEFAULT 0 COMMENT '积分'
);

-- 用户：不叫 user 是因为 user 在 MySQL/H2 里都是保留字，踩命名坑不如一开始就绕开
CREATE TABLE IF NOT EXISTS app_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    -- BCrypt 哈希固定 60 字符，留到 100 是防止将来换算法（如 Argon2）长度不够
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 唯一约束：防重复注册的最后防线。光靠代码先查再插是有并发漏洞的
    -- （两个请求同时查到「没人用这个名字」就都插进去了），数据库约束才是真正的守门员
    CONSTRAINT uk_app_user_username UNIQUE (username)
);

-- 比赛（球票卖的「商品」）。unit_price 单位是【分】—— 金额用整数存分，
-- 用浮点存元迟早出 0.1+0.2≠0.3 的账不平问题（工程手册 4.3：金额一律整数分）
CREATE TABLE IF NOT EXISTS match_info (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    home_team  VARCHAR(50) NOT NULL,
    away_team  VARCHAR(50) NOT NULL,
    match_time TIMESTAMP   NOT NULL,
    unit_price INT         NOT NULL COMMENT '单价（分）',
    status     VARCHAR(16) NOT NULL DEFAULT 'on_sale' COMMENT 'on_sale=在售 off_sale=下架'
);

-- 订单：order 是 SQL 保留字，所以叫 match_order
CREATE TABLE IF NOT EXISTS match_order (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- 订单号是给人和客服看的业务主键（20261004xxxxxx 这种），id 是数据库主键。
    -- 为什么不把 id 直接暴露给前端：自增 id 会泄漏业务量，还方便别人枚举遍历
    order_no     VARCHAR(32) NOT NULL,
    user_id      BIGINT      NOT NULL,
    match_id     BIGINT      NOT NULL,
    quantity     INT         NOT NULL,
    -- 下单时刻的单价快照：以后 match_info 改价，历史订单的金额不能跟着变。
    -- 「记录事实发生时的值」是订单系统的基本原则
    unit_price   INT         NOT NULL COMMENT '下单时单价快照（分）',
    total_amount INT         NOT NULL COMMENT '总价（分），后端重算，不信前端传的',
    status       VARCHAR(16) NOT NULL DEFAULT 'pending'
        COMMENT '状态机：pending→paid / pending→closed；closed 是终态（PRD 5.3）',
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_match_order_no UNIQUE (order_no)
);

-- 查「我的订单」永远带 user_id 条件，这个索引必须有。
-- 索引的意义等数据量大了才显现 —— 但等慢了再加，就是线上事故后补作业
CREATE INDEX idx_match_order_user ON match_order (user_id);
