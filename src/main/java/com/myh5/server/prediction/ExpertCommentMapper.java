package com.myh5.server.prediction;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论 Mapper。
 *
 * 除了 BaseMapper 的 CRUD，多一个「批量统计每条解析有几条评论」的聚合查询 ——
 * 这条 SQL 写在 mapper/ExpertCommentMapper.xml 里，不硬塞进注解。
 * 判断标准很简单：能用单行注解表达清楚的（如 exists 校验）就地写，
 * 一旦出现 <foreach> 这类动态元素，就该搬到 XML 去 —— 注解里拼 SQL 字符串没有语法高亮、
 * 没有格式化、改一个引号就崩，属于「能跑但没人愿意维护」的写法。
 */
@Mapper
public interface ExpertCommentMapper extends BaseMapper<ExpertCommentEntity> {

    /**
     * 批量统计评论数。
     * 关键在「批量」：如果按解析逐条 selectCount，6 条解析就是 6 次数据库往返（N+1 查询）。
     * 一次 IN 查询 + GROUP BY 换回全部计数，往返次数是常数 —— 这是最典型也最容易忽略的优化。
     *
     * @param analysisIds 解析 id 列表，调用方保证非空（空 IN () 是语法错误）
     */
    List<AnalysisCommentCountRow> countByAnalysisIds(@Param("analysisIds") List<Long> analysisIds);
}
