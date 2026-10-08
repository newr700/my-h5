package com.myh5.server.prediction;

import com.myh5.server.common.Result;
import com.myh5.server.prediction.dto.CreateCommentRequest;
import com.myh5.server.prediction.vo.DimScoreVo;
import com.myh5.server.prediction.vo.ExpertAnalysisVo;
import com.myh5.server.prediction.vo.ExpertCommentVo;
import com.myh5.server.prediction.vo.SeasonHistoryVo;
import com.myh5.server.prediction.vo.TeamPredictionVo;
import com.myh5.server.prediction.vo.TitleCountVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 英超预测接口（草图页面2 / 页面4 / 页面5）。
 *
 * ── 路径设计 ─────────────────────────────────────────────
 * 统一挂在 /prediction 下，按「数据域」分三段：
 *   /prediction/analysis  权威解析（页面2）
 *   /prediction/teams     AI 预测（页面4）
 *   /prediction/history   历史回顾（页面5）
 * 前三者是公开端点（逛网站不该被迫登录）；只有发表评论需要登录，
 * 且只有写路径在 AuthInterceptor 的保护名单里（见 WebMvcConfig）。
 *
 * ── 为什么保护的是「一个具体路径」而不是整个 /prediction/** ───────
 * 如果拦了整个前缀，未登录用户连权威解析都看不了 —— 那就成了「为了一个写接口
 * 把三个读接口一起关起来」。按最小必要范围授权，比图省事整段拦截更符合原则。
 * 代价是每加一个写接口都要记得往名单里加一条；
 * 当写接口多起来（比如超过五六个），就该换成「默认全拦 + 显式放行读列表」的写法 ——
 * 两种做法各有适用规模，知道什么时候该换，比记住哪一种「更对」重要。
 *
 * Controller 依旧薄得只剩「接参 → 调 Service → 包 Result」。
 */
@Tag(name = "英超预测")
@RestController
@RequestMapping("/prediction")
public class PredictionController {

    private final AnalysisService analysisService;
    private final PredictionService predictionService;
    private final HistoryService historyService;

    public PredictionController(AnalysisService analysisService,
                                PredictionService predictionService,
                                HistoryService historyService) {
        this.analysisService = analysisService;
        this.predictionService = predictionService;
        this.historyService = historyService;
    }

    // ── 页面2：权威解析 ────────────────────────────────────────

    @Operation(summary = "权威解析列表", description = "专家观点卡片，含各自评论条数；公开端点")
    @GetMapping("/analysis")
    public Result<List<ExpertAnalysisVo>> analysisList() {
        return Result.ok(analysisService.list());
    }

    @Operation(summary = "某条解析的评论列表", description = "按时间正序；公开端点")
    @GetMapping("/analysis/{id}/comments")
    public Result<List<ExpertCommentVo>> comments(@PathVariable long id) {
        return Result.ok(analysisService.comments(id));
    }

    @Operation(summary = "发表评论",
            description = "需要登录，且用户等级 ≥ 行业专家（Lv.2）；否则返回 6002")
    @PostMapping("/analysis/{id}/comment")
    public Result<ExpertCommentVo> comment(@PathVariable long id,
                                          @Valid @RequestBody CreateCommentRequest req) {
        return Result.ok(analysisService.addComment(id, req));
    }

    // ── 页面4：AI 预测 ────────────────────────────────────────

    @Operation(summary = "球队夺冠概率预测",
            description = "按六维加权模型算出夺冠概率并倒序排列；rank 由后端生成")
    @GetMapping("/teams")
    public Result<List<TeamPredictionVo>> teams() {
        return Result.ok(predictionService.listSorted());
    }

    @Operation(summary = "预测模型权重说明",
            description = "下发六个维度的权重，供页面展示算法依据；值即计算所用权重，不会与实现漂移")
    @GetMapping("/algorithm")
    public Result<List<DimScoreVo>> algorithm() {
        return Result.ok(predictionService.weights());
    }

    // ── 页面5：历史回顾 ────────────────────────────────────────

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
