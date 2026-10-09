package com.myh5.server.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 球队资料 Mapper：单表 CRUD 全靠 BaseMapper（字典表只有 20 行，一次全量取回） */
@Mapper
public interface FootballTeamMapper extends BaseMapper<FootballTeamEntity> {
}
