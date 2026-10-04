package com.myh5.server.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
