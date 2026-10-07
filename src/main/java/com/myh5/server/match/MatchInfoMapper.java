package com.myh5.server.match;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MatchInfoMapper extends BaseMapper<MatchInfoEntity> {

    // ============================================================
    // V2 第一步：防超卖（库存扣减）
    // ============================================================

    /**
     * 【整个防超卖的承重墙】原子扣减库存 —— 返回受影响的行数：1=成功，0=库存不足。
     *
     * ── 为什么这一条 UPDATE 就能防住超卖 ──
     * 关键点在 WHERE 里的 AND stock >= #{qty}：判断和写入发生在【同一条 SQL】里，
     * 数据库保证这条语句是原子的（InnoDB 会给这行加行锁），中间没有任何缝隙。
     *
     * ── 反面教材：先查再写为什么不行 ──
     * 新手的自然写法是两步：
     *      MatchInfo m = selectById(id);          // ① 查：还剩 1 张
     *      if (m.getStock() >= qty)               // ② 判断：够了
     *          update ... set stock = stock - qty; // ③ 写
     * 单线程看着没问题，但只要两个请求并发执行，它就会超卖：
     *
     *     时刻        请求 A                          请求 B
     *     ─────────────────────────────────────────────────────
     *     t1       ① 查：stock = 1
     *     t2                                      ① 查：stock = 1   ← A 还没写，B 也看到 1
     *     t3       ② 判断：1 >= 1 通过
     *     t4                                      ② 判断：1 >= 1 通过  ← 两个都通过！
     *     t5       ③ 写：stock = 0
     *     t6                                      ③ 写：stock = -1  ← 超卖了
     *
     * 这就是经典的 **check-then-act 竞态（TOCTOU）**：「检查」和「行动」之间存在时间缝隙。
     * 这个 bug 单测几乎抓不到（测试是串行的），只有真实并发或压测才暴露，
     * 而且排查极其痛苦 —— 因为它不是每次都复现。
     *
     * ── 这一条 UPDATE 为什么没有缝隙 ──
     * InnoDB 执行 UPDATE 时会先给目标行加【排他锁（行锁）】，直到事务提交才释放。
     * 所以 B 的 UPDATE 必须在 A 提交之后才能执行，此时它重新读到的是 A 扣完的新值：
     *
     *     t1  A: UPDATE ... WHERE id=1 AND stock >= 1   → 加锁，stock: 1 → 0，影响 1 行
     *     t2  B: UPDATE ... WHERE id=1 AND stock >= 1   → 等待 A 的锁
     *     t3  A: 提交，释放锁
     *     t4  B: 拿到锁，读到 stock = 0，0 >= 1 不成立 → 影响 0 行 → 判库存不足 ✓
     *
     * 【务必注意】这个保证来自数据库的事务机制，所以外层方法必须带 @Transactional：
     * 没有事务时每条语句自动提交，锁的持有时间缩短到语句本身，
     * 虽仍有行锁保护单次 UPDATE，但一旦逻辑扩展到「扣库存 + 建订单」多步操作，
     * 原子性就没了。所以见 OrderService.create 上的 @Transactional。
     *
     * ── 为什么用「条件 UPDATE + 判断影响行数」，而不是别的方式 ──
     *   方案① 乐观锁（加 version 列，UPDATE ... WHERE version = ?）
     *          同样的思路，多一列、多次重试；本场景因为本身就要做 UPDATE 判断，直接用条件更省。
     *   方案② 分布式锁（Redis SETNX）
     *          跨多个服务/多种资源争抢时才需要；单库场景下数据库行锁已经是最强保证，
     *          再套一层分布式锁属于过度设计（而且引入新的失效/续期复杂度）。
     *   方案③ SELECT ... FOR UPDATE（先悲观锁再更新）
     *          可行，但要两条语句往返，且锁的范围与时间更长。
     *   本项目选【条件 UPDATE】，是因为它语句最少、依赖最少、而保证最强。
     *
     * @param qty 购买数量
     * @return 1=扣减成功；0=库存不足（调用方据此抛 STOCK_NOT_ENOUGH）
     */
    @Update("UPDATE match_info SET stock = stock - #{qty} WHERE id = #{matchId} AND stock >= #{qty}")
    int deductStock(@Param("matchId") long matchId, @Param("qty") int qty);

    /**
     * 回补库存（订单取消/关闭时把票还给票池）。
     *
     * 【为什么必须回补】只在下单时扣、取消时不还的话，
     * 用户反复「下单 → 取消」几次就能把票池耗尽 —— 票没卖出去，却谁也买不了了。
     * 这叫库存泄漏，和内存泄漏是一个道理：借了不还。
     *
     * 【为什么这里不加 AND 条件】加回来没有「上限」风险吗？
     * 严格来说应该加 total_stock 上限保护：stock + qty <= total_stock，
     * 防止重复回补（比如重试了两次）把剩余票数刷到比总数还多。
     * 本实现没加，是因为回补只发生在「pending → closed」这一次状态转移里，
     * 而 pay/cancel 会先校验当前状态是 pending（见 OrderService.cancel），
     * 重复调用第二次会被状态机守卫挡下 —— 等于用状态机兜住了重复回补。
     * （面试追问时的标准答案：如果要更严，可以在这里加 .AND stock + #{qty} <= total_stock 形成自保护，
     *  代价是 SQL 读起来没那么直白。）
     *
     * @return 受影响行数（1=成功回补，0=比赛不存在）
     */
    @Update("UPDATE match_info SET stock = stock + #{qty} WHERE id = #{matchId}")
    int restoreStock(@Param("matchId") long matchId, @Param("qty") int qty);
}
