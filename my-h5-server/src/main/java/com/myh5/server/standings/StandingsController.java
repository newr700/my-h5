package com.myh5.server.standings;

import com.myh5.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 积分榜接口（PRD-F1）。公开端点。
 * Controller 薄得只剩「接参 → 调 Service → 包 Result」—— 这是它应该有的样子。
 */
@Tag(name = "积分榜")
@RestController
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @Operation(summary = "获取积分榜", description = "按 积分→净胜球→进球→队名 排序，rank 由后端生成")
    @GetMapping("/standings")
    public Result<List<StandingVo>> list() {
        return Result.ok(standingsService.listSorted());
    }
}
