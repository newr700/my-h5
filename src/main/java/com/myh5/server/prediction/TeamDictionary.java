package com.myh5.server.prediction;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 球队字典 —— 把 football_team 表读成「队名 → 资料」的 Map。
 *
 * 【为什么值得单独一个类】
 * 三个新页面的服务（权威解析 / AI 预测 / 历史回顾）都要做同一件事：
 * 业务表里存的是队名，展示时要换成「颜色 + 缩写 + 队徽」。
 * 这份逻辑写三遍的后果不是「多写几行」，而是「某天改了兜底规则，只改了两处」——
 * 页面 A 缺字典行时显示灰色盾牌，页面 B 直接白屏，排查半天才发现是复制粘贴漏了一处。
 *
 * 所以：凡是「同一段逻辑会在两处以上出现」就抽出来。
 * 这里抽的粒度是一个 @Component，注入即用，不需要知道它内部查了几张表。
 */
@Component
public class TeamDictionary {

    private final FootballTeamMapper teamMapper;

    public TeamDictionary(FootballTeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    /**
     * 读全量字典并转成 Map。
     *
     * 为什么直接 selectList(null) 全量取：这是 20 行的字典表，
     * 加 WHERE 条件过滤反而多一次解析成本；而且字典表几乎不变，
     * 真到了性能敏感的阶段，第一选择是加缓存而不是加查询条件。
     *
     * 合并函数 (a, b) -> a 是防护：万一数据库里出现了重名队（唯一索引挡着，
     * 但迁移脚本出过错的库不一定有），Collectors.toMap 会抛异常导致整个接口 500。
     * 取先出现的那条，让请求正常返回 —— 数据问题应该在数据层修，不该炸到用户脸上。
     */
    public Map<String, FootballTeamEntity> asMap() {
        return teamMapper.selectList(null).stream()
                .collect(Collectors.toMap(FootballTeamEntity::getTeamName, t -> t, (a, b) -> a));
    }
}
