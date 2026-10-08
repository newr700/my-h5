package com.myh5.server.prediction;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.myh5.server.auth.AuthContext;
import com.myh5.server.common.BizException;
import com.myh5.server.common.ErrorCodes;
import com.myh5.server.prediction.dto.CreateCommentRequest;
import com.myh5.server.prediction.vo.ExpertAnalysisVo;
import com.myh5.server.prediction.vo.ExpertCommentVo;
import com.myh5.server.user.UserEntity;
import com.myh5.server.user.UserLevels;
import com.myh5.server.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权威解析服务（页面2）。
 *
 * 三件事：
 *   ① 列表：专家观点卡片 + 各自的评论条数（一次 IN 聚合查询，不做 N+1）；
 *   ② 评论列表：点开某条解析才拉它的评论（按需加载，首屏不用背全部评论）；
 *   ③ 发表评论：等级校验 → 存在性校验 → 落库。这是本模块唯一的写路径。
 */
@Service
public class AnalysisService {

    private final ExpertAnalysisMapper analysisMapper;
    private final ExpertCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final TeamDictionary teamDictionary;

    public AnalysisService(ExpertAnalysisMapper analysisMapper,
                           ExpertCommentMapper commentMapper,
                           UserMapper userMapper,
                           TeamDictionary teamDictionary) {
        this.analysisMapper = analysisMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.teamDictionary = teamDictionary;
    }

    /** 权威解析列表：按 sort_order 排（人工编排的展示顺序），同序时用 id 兜底保证结果确定 */
    public List<ExpertAnalysisVo> list() {
        List<ExpertAnalysisEntity> analyses = analysisMapper.selectList(
                new QueryWrapper<ExpertAnalysisEntity>().orderByAsc("sort_order", "id"));
        if (analyses.isEmpty()) {
            // 提前返回不只是省一次查询：下面要拼 IN 条件，空列表会拼出 IN () 的语法错误
            return List.of();
        }

        // 三个数据来源各查一次，然后内存里拼：
        //   ① 评论数：一条 IN + GROUP BY 换回全部计数
        //   ② 球队字典：颜色/缩写/队徽
        Map<Long, Integer> commentCounts = commentMapper
                .countByAnalysisIds(analyses.stream().map(ExpertAnalysisEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(AnalysisCommentCountRow::getAnalysisId,
                        row -> row.getCommentCount() == null ? 0 : row.getCommentCount()));

        Map<String, FootballTeamEntity> teamMap = teamDictionary.asMap();

        return analyses.stream()
                .map(a -> toVo(a, teamMap.get(a.getTeamName()),
                        commentCounts.getOrDefault(a.getId(), 0)))
                .toList();
    }

    /**
     * 某条解析下的评论，按时间正序（像一段对话那样从头往下读）。
     *
     * 为什么不加分页：本页评论量级是个位数到几十条，一次取完最简单也最省事。
     * 什么时候必须分页？当「一条解析可能有几千条评论」时 —— 那是产品形态变了，
     * 而不是现在这个「专家点评 + 少量讨论」的形态。按当前形态做设计，不提前上重武器。
     */
    public List<ExpertCommentVo> comments(long analysisId) {
        return commentMapper.selectList(new QueryWrapper<ExpertCommentEntity>()
                        .eq("analysis_id", analysisId)
                        .orderByAsc("created_at", "id"))
                .stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 发表评论 —— 本模块唯一的写路径，也是权限校验真正生效的地方。
     *
     * 校验顺序刻意是「先等级、后存在性」：
     * 等级不够的人连「这条解析存不存在」都不需要知道。
     * 反过来先查存在性，等于给不满足条件的调用者多喂了一个信息
     * （虽然这里不敏感，但养成「把最便宜的、最该先拒的判断放前面」的习惯是好事：
     *  越早拒绝，越少代码被执行，也越少信息被泄露）。
     *
     * @Transactional 的必要性：这里只有一条 INSERT，看起来不需要事务。
     * 但要留一个心眼 —— 一旦以后加了「发评论顺便给专家加积分」这类需求，
     * 没有事务就会写出「评论发了、积分没加」的半成品状态。
     * 单条写操作上事务成本几乎为零，而漏加的排查成本很高，所以照加不误。
     */
    @Transactional
    public ExpertCommentVo addComment(long analysisId, CreateCommentRequest req) {
        Long userId = AuthContext.requireUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            // token 合法但账号没了（被删）：按未登录处理，与 UserController.profile 同一口径
            throw new BizException(ErrorCodes.UNAUTHORIZED, "账号不存在，请重新登录");
        }

        // ── 等级守卫：后端是唯一裁判 ──────────────────────────────
        // 前端也会藏起评论框，但那只是体验；能改 JS 的人照样能打这个接口，
        // 所以真正拦住「普通球迷发言」的是下面这三行。
        int level = user.getUserLevel() == null ? UserLevels.NORMAL : user.getUserLevel();
        if (level < UserLevels.EXPERT) {
            throw new BizException(ErrorCodes.COMMENT_LEVEL_INSUFFICIENT,
                    "仅行业专家（Lv." + UserLevels.EXPERT + "）及以上可发表评论");
        }

        if (analysisMapper.selectById(analysisId) == null) {
            throw new BizException(ErrorCodes.ANALYSIS_NOT_FOUND, "这条权威解析不存在或已下架");
        }

        ExpertCommentEntity comment = new ExpertCommentEntity();
        comment.setAnalysisId(analysisId);
        comment.setUserId(userId);
        // 昵称与等级存【快照】：等级是「当时他有资格说这句话」的证据，
        // 事后升降级不该改写历史（详见 ExpertCommentEntity 的注释）
        comment.setNickname(user.getNickname());
        comment.setUserLevel(level);
        // 首尾空格去掉：用户误敲的空格不该进入数据，否则排序、去重、“空评论”判断全受影响
        comment.setContent(req.content().trim());
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        return toVo(comment);
    }

    /** 实体 + 球队资料 + 评论数 → VO */
    private ExpertAnalysisVo toVo(ExpertAnalysisEntity a, FootballTeamEntity team, int commentCount) {
        return new ExpertAnalysisVo(
                a.getId(),
                a.getExpertNameEn(),
                a.getExpertNameCn(),
                a.getTeamName(),
                team == null ? "" : team.getTeamNameEn(),
                team == null ? "TBD" : team.getShortName(),
                team == null ? "#909399" : team.getColorPrimary(),
                team == null ? "#C0C4CC" : team.getColorSecondary(),
                team == null || team.getLogoUrl() == null ? "" : team.getLogoUrl(),
                a.getReason(),
                // 契约：没头像给 ''，前端用姓名首字兜底
                a.getAvatarUrl() == null ? "" : a.getAvatarUrl(),
                commentCount
        );
    }

    private ExpertCommentVo toVo(ExpertCommentEntity c) {
        return new ExpertCommentVo(c.getId(), c.getAnalysisId(), c.getUserId(),
                c.getNickname(), c.getUserLevel(), c.getContent(), c.getCreatedAt());
    }
}
