package com.myh5.server.match;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myh5.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 比赛接口（PRD-F4）。公开端点 —— 看球票不需要登录，买才需要。
 */
@Tag(name = "比赛")
@RestController
public class MatchController {

    private final MatchInfoMapper matchInfoMapper;

    public MatchController(MatchInfoMapper matchInfoMapper) {
        this.matchInfoMapper = matchInfoMapper;
    }

    @Operation(summary = "获取在售比赛列表", description = "按开赛时间升序，只返回在售场次")
    @GetMapping("/matches")
    public Result<List<MatchVo>> listOnSale() {
        List<MatchInfoEntity> entities = matchInfoMapper.selectList(
                new LambdaQueryWrapper<MatchInfoEntity>()
                        .eq(MatchInfoEntity::getStatus, "on_sale")
                        .orderByAsc(MatchInfoEntity::getMatchTime)
        );
        // 这里没有写 XML：单表 + 简单条件，正是 MP 该干的活（复杂查询才手写 XML，见 OrderMapper.xml）
        return Result.ok(entities.stream().map(MatchVo::from).toList());
    }
}
