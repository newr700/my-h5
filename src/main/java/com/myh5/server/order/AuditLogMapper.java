package com.myh5.server.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper —— 纯单表 CRUD，继承 BaseMapper 即可，无需手写 SQL。
 * 审计的写入与查询都由它完成，调用方是 AuditLogService。
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
}
