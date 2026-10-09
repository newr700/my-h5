package com.myh5.server.prediction;

import com.myh5.server.common.Result;
import com.myh5.server.prediction.vo.DimScoreVo;
import com.myh5.server.prediction.vo.TeamPredictionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 页面4 AI 预测接口（球队夺冠概率排名 + 评估维度说明）。
 *
 * 统一挂在 /prediction 下，均为公开端点。Controller 薄得只剩「接参 → 调 Service → 包 Result」。
 */
@Tag(name = "AI 预测")
@RestController
@RequestMapping("/prediction")
public class TeamPredictionController {

    private final TeamPredictionService teamPredictionService;

    public TeamPredictionController(TeamPredictionService teamPredictionService) {
        this.teamPredictionService = teamPredictionService;
    }

    @Operation(summary = "球队夺冠概率预测",
            description = "按文档给定的夺冠概率倒序排列；rank 由后端生成")
    @GetMapping("/teams")
    public Result<List<TeamPredictionVo>> teams() {
        return Result.ok(teamPredictionService.listSorted());
    }

    @Operation(summary = "AI 模型评估维度说明",
            description = "下发给页面展示的评估维度定义（名称与顺序即雷达图顶点）；概率由模型直接给出，非加权算出")
    @GetMapping("/algorithm")
    public Result<List<DimScoreVo>> algorithm() {
        return Result.ok(teamPredictionService.weights());
    }
}
