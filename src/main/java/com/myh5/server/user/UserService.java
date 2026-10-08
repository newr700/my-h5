package com.myh5.server.user;

import com.myh5.server.common.BizException;
import com.myh5.server.common.ErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 用户业务层 —— 把「上传头像」这件涉及文件 IO + DB 更新的事从 Controller 抽出来，
 * 保持 Controller 只负责收参数、调服务、包结果（分层红线：Controller→Service→Mapper）。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final String uploadDir;

    // 只允许这三种图片类型 —— 文件上传的第一条安全纪律：防用户传 exe / 脚本等危险文件
    private static final List<String> ALLOWED_TYPES =
            Arrays.asList("image/jpeg", "image/png", "image/webp");
    // 对应扩展名，写盘时用；Content-Type 可被伪造，扩展名再兜一道
    private static final List<String> ALLOWED_EXT =
            Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    public UserService(UserMapper userMapper,
                       @Value("${app.upload-dir}") String uploadDir) {
        this.userMapper = userMapper;
        this.uploadDir = uploadDir;
    }

    /**
     * 读当前用户资料（含等级）。
     *
     * 把「查不到人怎么办」的判断收进 Service，让 Controller 彻底变成三行 ——
     * 顺带的好处是这个判断只有一处，profile 与 expertApply 不会出现两种口径。
     */
    public UserProfileVo profile(Long userId) {
        return toVo(requireUser(userId));
    }

    /**
     * 申请成为行业专家（V7 新增）。
     *
     * ⚠️ 这是一处【刻意的简化】，必须写清楚：
     * 真实系统里「升级行业专家」应该是提交资料 → 管理员审批 → 生效的流程，
     * 并且审批接口只能由管理员调用。本项目没有后台管理端，为了让他把
     * 「高等级用户可发评论」这个功能亲眼跑通（而不是永远卡在 Lv.1 看不到输入框），
     * 这里做成「一点即通过」的模拟审核。
     *
     * 升级后必须【回读数据库】再返回，不能用内存里改过的对象：
     * updateById 实际是否写成功、有没有被别的字段覆盖，只有再查一次才说了算 ——
     * 「写完立刻假装成功」是接口骗人最常见的形态（前端显示已升级，刷新又变回去）。
     */
    @Transactional
    public UserProfileVo applyExpert(Long userId) {
        UserEntity user = requireUser(userId);
        int current = user.getUserLevel() == null ? UserLevels.NORMAL : user.getUserLevel();

        // 已经是专家就不重复写库：写操作要「幂等」——同一个请求打两次，结果和副作用都该一样
        if (current < UserLevels.EXPERT) {
            UserEntity update = new UserEntity();
            update.setId(userId);
            update.setUserLevel(UserLevels.EXPERT);
            // updateById 默认只更新非空字段，所以这里只会改 user_level 一列
            userMapper.updateById(update);
            user = requireUser(userId);
        }
        return toVo(user);
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            // token 合法但账号没了：按未登录处理（与 UserController 原来的口径一致）
            throw new BizException(ErrorCodes.UNAUTHORIZED, "账号不存在，请重新登录");
        }
        return user;
    }

    /** Entity → VO 的唯一出口：等级缺失时按最低等级兜底，绝不下发 null 让前端判空 */
    private UserProfileVo toVo(UserEntity user) {
        int level = user.getUserLevel() == null ? UserLevels.NORMAL : user.getUserLevel();
        return new UserProfileVo(user.getId(), user.getUsername(), user.getNickname(),
                user.getCreatedAt(), user.getAvatarUrl(), level);
    }

    /**
     * 上传头像：校验 → 写盘 → 删旧图 → 更新 DB 字段 → 返回相对路径。
     * 返回 /uploads/xxx.png 这种相对路径（不带域名），由前端按部署环境拼成可访问 URL。
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        // ① 校验非空
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCodes.FILE_EMPTY, "请选择要上传的图片");
        }
        // ② 校验类型（Content-Type 可被伪造，作为第一道门槛；扩展名在 ③ 兜底）
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BizException(ErrorCodes.FILE_TYPE_INVALID, "仅支持 JPG / PNG / WebP 图片");
        }

        // ③ 取扩展名（从原文件名，防止 Content-Type 与实际不符导致存错后缀）
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf(".")).toLowerCase()
                : ".png";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException(ErrorCodes.FILE_TYPE_INVALID, "仅支持 JPG / PNG / WebP 图片");
        }

        // ④ 落盘目录不存在则创建（首次上传时）
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException(ErrorCodes.SYSTEM_ERROR, "头像目录创建失败");
        }

        // ⑤ 文件名：userId + 时间戳 + 随机，避免重名覆盖 + 防遍历（绝不直接用原文件名）
        String filename = userId + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ext;
        File dest = new File(dir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BizException(ErrorCodes.SYSTEM_ERROR, "头像保存失败，请重试");
        }

        // ⑥ 删旧头像文件（避免磁盘无限膨胀）。DB 只存相对路径，解析出文件名即可
        UserEntity existing = userMapper.selectById(userId);
        if (existing != null && existing.getAvatarUrl() != null) {
            String oldName = existing.getAvatarUrl().replace("/uploads/", "");
            File oldFile = new File(dir, oldName);
            if (oldFile.exists()) {
                oldFile.delete();
            }
        }

        // ⑦ 更新 DB 字段（只更新 avatar_url，updateById 只改非空字段）
        UserEntity update = new UserEntity();
        update.setId(userId);
        update.setAvatarUrl("/uploads/" + filename);
        userMapper.updateById(update);

        return "/uploads/" + filename;
    }
}
