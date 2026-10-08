package com.myh5.server.match;

import com.myh5.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 比赛接口（PRD-F4）。公开端点 —— 看球票不需要登录，买才需要。
 *
 * 这是 Controller 该有的样子（参照 StandingsController 范本）：
 *   ① 只接 HTTP 参数（本端点无参）；
 *   ② 把活交给 MatchService；
 *   ③ 用 Result 包好返回。
 * 不写任何业务逻辑、不碰 Mapper —— 数据库读写归 Service，这是《工程实施手册》3.2 的分层红线。
 */
@Tag(name = "比赛")
@RestController
public class MatchController {

    private final MatchService matchService;

    /** 构造器注入 Service（注意：注入的是 Service，不是 Mapper） */
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @Operation(summary = "获取在售比赛列表", description = "按开赛时间升序，只返回在售场次")
    @GetMapping("/matches")
    public Result<List<MatchVo>> listOnSale() {
        // 三层职责拆干净：接参（无）→ 调 Service（业务在 Service 里）→ 包 Result
        return Result.ok(matchService.listOnSale());
    }
}
