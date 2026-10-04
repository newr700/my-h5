package com.myh5.server.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 分页响应 —— 形状按《工程实施手册》4.3 裁定：{ list, total, pageNum, pageSize }。
 *
 * 为什么分页参数从 1 开始（pageNum=1 是第一页）而不是从 0：
 * 这是给人看的语义（"第 1 页"），offset 从 0 开始是给机器看的。
 * 前后端统一从 1 开始，转化只发生在一个地方（MP 的 Page 构造器也是 1 起始），
 * 混用 0 和 1 是分页 bug 的头号来源。
 */
public record PageResult<T>(
        List<T> list,
        long total,
        long pageNum,
        long pageSize
) {
    /** 从 MyBatis-Plus 的分页对象转换 —— 全项目只允许在这里碰 IPage */
    public static <T> PageResult<T> from(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
