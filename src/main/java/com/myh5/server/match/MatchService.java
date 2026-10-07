package com.myh5.server.match;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 比赛服务（PRD-F4）。
 *
 * 这一层为什么必须存在？MatchController 之前直接注入了 MatchInfoMapper、在 Controller 里查库，
 * 那等于把 Controller 和 Service 合二为一了，违反了《工程实施手册》3.2 的「分层红线」。
 * 红线规定：Controller 只干三件事 —— 接参数、调 Service、包 Result，绝不能碰 Mapper。
 *
 * 让 Controller 直接读数据库，三个具体坏处：
 *   ① 业务规则（"在售"是什么状态、按什么排序）散落在 Controller，换一个端点就复用不了；
 *   ② Controller 直接碰数据库，单元测试要起整个 Web 容器，没法把"查列表"这个逻辑单独测；
 *   ③ 哪天要在"查列表"前后加校验 / 缓存 / 埋点，得改 Controller —— 它本不该懂这些。
 *
 * 抽成 Service 后职责就清晰了：数据库怎么读归 Service，Controller 只负责
 * "把一次 HTTP 请求翻译成一次 Service 调用"。这正是软件工程里"单一职责 + 关注点分离"的体现。
 */
@Service
public class MatchService {

    private final MatchInfoMapper matchInfoMapper;

    /** 构造器注入 Mapper（Spring 推荐，final 字段 + 单构造器 = 不可变、好测试） */
    public MatchService(MatchInfoMapper matchInfoMapper) {
        this.matchInfoMapper = matchInfoMapper;
    }

    /**
     * 获取在售比赛列表。
     *
     * 查询条件：status = 'on_sale'（在售）+ 按开赛时间升序。
     * 单表 + 简单等值 / 排序条件，正是 MyBatis-Plus 该干的活 —— 不用写 XML。
     * （复杂查询、连表、计算列才手写 XML，见 order/OrderMapper.xml）
     *
     * Entity → VO 的转换也收口在本层：matchTitle 这种"业务展示规则"
     * （"球队1 vs 球队2"）放在后端拼，前端只渲染；哪天文案要改成"【中超】球队1 VS 球队2"，只动后端一处。
     */
    public List<MatchVo> listOnSale() {
        List<MatchInfoEntity> entities = matchInfoMapper.selectList(
                new LambdaQueryWrapper<MatchInfoEntity>()
                        .eq(MatchInfoEntity::getStatus, "on_sale")
                        .orderByAsc(MatchInfoEntity::getMatchTime)
        );
        // 实体 → VO：内部模型翻译成契约模型，字段差异都在这一个方法里收口
        return entities.stream().map(MatchVo::from).toList();
    }
}
