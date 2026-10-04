package com.myh5.server.standings;

import com.myh5.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 积分榜模块（对应 PRD-F1）。
 *
 * 骨架阶段：返回 3 条示例数据，用于打通「前端 5173 → 代理 → 8080」整条链路。
 * 示例数据刻意只有 3 条 —— 前端 expectCount(list, 'standings', 20) 的运行时校验
 * 会报警告但不崩（宽松模式），正好顺便验证校验机制活着。
 */
@Tag(name = "积分榜")
@RestController
public class StandingsController {

    @Operation(summary = "获取积分榜", description = "返回按积分规则排好序的球队列表，rank 由后端生成")
    @GetMapping("/standings")
    public Result<List<StandingVo>> list() {
        // TODO(W1): 接入 MySQL 后改为查库，排序规则见 PRD-F1（积分→净胜球→进球→队名）
        List<StandingVo> demo = List.of(
                new StandingVo(1, "曼城", 7, 5, 2, 0, 18, 6, 17, ""),
                new StandingVo(2, "阿森纳", 7, 5, 1, 1, 15, 7, 16, ""),
                new StandingVo(3, "利物浦", 7, 4, 2, 1, 14, 8, 14, "")
        );
        return Result.ok(demo);
    }
}
