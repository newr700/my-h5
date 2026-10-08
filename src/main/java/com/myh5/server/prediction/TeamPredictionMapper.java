package com.myh5.server.prediction;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 六维评分 Mapper。
 *
 * 注意：这里【没有】按夺冠概率排序的查询 —— 因为概率不是存储列，
 * 排序只能由 Service 在算出概率之后做（见 PredictionService.listSorted 的注释）。
 */
@Mapper
public interface TeamPredictionMapper extends BaseMapper<TeamPredictionEntity> {
}
