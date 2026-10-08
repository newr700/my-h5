-- ============================================================
-- V5 【V2 第三步：审计日志】新增 audit_log 表
-- ------------------------------------------------------------
-- 【解决什么】
-- Step1 防超卖（V3）、Step2 防重复下单（V4）分别解决了「正确性」，
-- 这一步解决「事后说不清」：谁、什么时间、对哪笔订单、做了什么、
-- 成功还是失败、失败的错误码是什么。没有这层留痕，线上出资损或客诉，
-- 只能靠猜；有了它，对账与排错才有据可查。
--
-- 【记什么（字段设计）】
-- user_id        操作人
-- action         动作：CREATE_ORDER / PAY / CANCEL（枚举，见 OrderService 调用处）
-- target_type    对象类型，当前固定 ORDER（为以后扩展留口）
-- target_id      对象 id，如下单记录 order_id
-- before_status  操作前状态（CREATE 没有前态，为 NULL）
-- after_status   操作后状态
-- result         1 成功 / 0 失败
-- error_code     失败时的业务错误码（成功为 NULL）
-- request_id     关联幂等号（与 V4 打通，便于串联排查同一笔下单）
-- detail         自由备注（如购买数量、失败原因）
-- created_at     操作时间（默认当前时间）
--
-- 【为什么审计表【不】建外键指向 match_order】
-- 审计的核心价值之一是「业务主事务回滚了，审计记录也要留下来」
-- （例如下单因库存不足整体回滚，但我们仍想记下「这次失败的下单尝试」）。
-- 若有外键，主表记录随事务消失，审计行会违反外键约束插不进去 ——
-- 反而丢了最该留的失败痕迹。所以审计表是「弱依赖」的，只存 id 不约束。
--
-- 【兼容性】同 V1~V4：只用 MySQL 8 与 H2（MySQL 模式）都支持的写法。
-- ============================================================

CREATE TABLE audit_log (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT       NOT NULL COMMENT '操作人',
  action        VARCHAR(32)  NOT NULL COMMENT '动作: CREATE_ORDER / PAY / CANCEL',
  target_type   VARCHAR(32)  NOT NULL DEFAULT 'ORDER' COMMENT '对象类型，当前固定 ORDER',
  target_id     BIGINT       COMMENT '对象 id，如下单记录 order_id',
  before_status VARCHAR(32)  COMMENT '操作前状态（CREATE 无此前态）',
  after_status  VARCHAR(32)  COMMENT '操作后状态',
  result        TINYINT      NOT NULL COMMENT '1 成功 / 0 失败',
  error_code    INT          COMMENT '失败时的业务错误码，成功为 NULL',
  request_id    VARCHAR(64)  COMMENT '关联幂等号（与 V4 打通）',
  detail        VARCHAR(512) COMMENT '补充说明：数量 / 失败原因等',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
);

CREATE INDEX idx_audit_log_user    ON audit_log(user_id);
CREATE INDEX idx_audit_log_target  ON audit_log(target_type, target_id);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);
