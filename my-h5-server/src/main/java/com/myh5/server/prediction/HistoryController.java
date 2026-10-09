package com.myh5.server.prediction;

import com.myh5.server.common.Result;
import com.myh5.server.prediction.vo.SeasonHistoryVo;
import com.myh5.server.prediction.vo.TitleCountVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 页面5 历史回顾接口（历届战绩 + 全时期夺冠次数）。
 *
 * 统一挂在 /prediction 下，均为公开端点。Controller 薄得只剩「接参 → 调 Service → 包 Result」。
 */
@Tag(name = "历史回顾")
@RestController
@RequestMapping("/prediction")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Operation(summary = "历届战绩", description = "按届数倒序，含冠亚季军球队信息")
    @GetMapping("/history")
    public Result<List<SeasonHistoryVo>> history() {
        return Result.ok(historyService.listSeasons());
    }

    @Operation(summary = "2000 年以来夺冠次数 Top5",
            description = "由 season_history 现算（GROUP BY + COUNT），追加赛季后自动更新")
    @GetMapping("/history/titles")
    public Result<List<TitleCountVo>> titleCounts() {
        return Result.ok(historyService.topTitleCounts());
    }
}
