package com.myh5.server.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单 Mapper。
 * 单表 CRUD 用 BaseMapper；「我的订单列表」需要 JOIN 比赛表取队名 ——
 * 按工程手册裁定，复杂查询手写 XML（mapper/OrderMapper.xml），
 * SQL 看得见摸得着，面试时也能指着它讲 JOIN 和索引。
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    /**
     * 分页查「我的订单」（联 match_info 拼出比赛标题和开赛时间）。
     *
     * IPage 参数是 MP 分页插件的约定：拦截器看到第一个参数是 IPage，
     * 就自动给 XML 里的 SQL 包 LIMIT，并额外发 COUNT 查询 ——
     * 所以 XML 里【不要】自己写 LIMIT。
     */
    IPage<OrderDetailRow> selectMyOrders(IPage<OrderDetailRow> page, @Param("userId") Long userId);

    /**
     * 按 (用户, 幂等号) 查是否已有订单 —— 幂等预检的核心查询。
     * request_id 列可为 NULL（V1/V2/V3 的老订单），数据库唯一索引对 NULL 不过滤，
     * 所以这里用精确等值匹配即可；新订单的 requestId 由 @NotBlank 保证非空。
     */
    @Select("SELECT * FROM match_order WHERE user_id = #{userId} AND request_id = #{requestId} LIMIT 1")
    OrderEntity selectByUserAndRequest(@Param("userId") Long userId, @Param("requestId") String requestId);
}
