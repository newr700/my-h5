package com.myh5.server.standings;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 积分榜 Mapper：单表 CRUD 全靠 BaseMapper，排序条件用 QueryWrapper 在 Service 里给 */
@Mapper
public interface StandingsMapper extends BaseMapper<TeamStandingEntity> {
}
