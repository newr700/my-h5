package com.myh5.server.user;

import com.myh5.server.common.BizException;
import com.myh5.server.common.ErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
