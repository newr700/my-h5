package com.myh5.server.prediction;

import com.myh5.server.common.Result;
import com.myh5.server.prediction.dto.CreateCommentRequest;
import com.myh5.server.prediction.vo.ExpertAnalysisVo;
import com.myh5.server.prediction.vo.ExpertCommentVo;
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
 * 页面2 权威解析接口。
 *
 * 统一挂在 /prediction 下。列表与评论是公开端点（逛网站不该被迫登录）；
 * 只有发表评论需要登录，且只有写路径在 AuthInterceptor 的保护名单里（见 WebMvcConfig）。
 *
 * Controller 依旧薄得只剩「接参 → 调 Service → 包 Result」。
 */
@Tag(name = "权威解析")
@RestController
@RequestMapping("/prediction")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

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
}
