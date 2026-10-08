package com.myh5.server.prediction;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 明星球员 Mapper（一次查全部再按队名分组，避免「循环里查库」的 N+1） */
@Mapper
public interface TeamStarPlayerMapper extends BaseMapper<TeamStarPlayerEntity> {
}
