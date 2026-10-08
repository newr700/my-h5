package com.myh5.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【头像上传】专项集成测试。
 *
 * ── 锁住什么 ─────────────────────────────────────────────
 * ① 上传成功返回 /uploads/ 开头的相对路径；② 文件真的落盘到磁盘；
 * ③ 再传一次旧文件被删（防磁盘膨胀）；④ profile 接口把 avatarUrl 带回前端；
 * ⑤ 非图片类型被拒（4002）；⑥ 空文件被拒（4001）。
 *
 * ── 隔离 ───────────────────────────────────────────────
 * 用专用用户名 avatar-tester 注册，避免与其他测试类共享账号 / 头像文件互相覆盖。
 * 上传目录走测试配置 target/test-uploads（见 src/test/resources/application.yml），
 * 与 WebMvcConfig 映射的 file:target/test-uploads 一致，跑完即焚。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AvatarUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final ObjectMapper om = new ObjectMapper();
    private static String token;

    /** 注册一个专用测试用户（用户名 4-32 位，符合 RegisterRequest 的 @Size） */
    private String registerAndGetToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username
                                + "\",\"password\":\"123456\",\"nickname\":\"头像测试\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        return body.get("data").get("token").asText();
    }

    @Test
    @Order(1)
    void 注册用户拿到token() throws Exception {
        token = registerAndGetToken("avatar-tester");
    }

    @Test
    @Order(2)
    void 上传头像成功_返回相对路径且文件落盘() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "head.png", "image/png", new byte[]{1, 2, 3, 4, 5});
        MvcResult result = mockMvc.perform(multipart("/user/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt(), "上传应成功");

        String avatarUrl = body.get("data").asText();
        assertTrue(avatarUrl.startsWith("/uploads/"), "返回应为 /uploads/ 开头的相对路径");
        // 文件真的写到了磁盘（不只是 DB 里一行字）
        String filename = avatarUrl.replace("/uploads/", "");
        File onDisk = new File(uploadDir, filename);
        assertTrue(onDisk.exists(), "头像文件应已落盘：" + onDisk.getAbsolutePath());
    }

    @Test
    @Order(3)
    void profile接口带回avatarUrl() throws Exception {
        MvcResult result = mockMvc.perform(get("/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        String avatarUrl = body.get("data").get("avatarUrl").asText();
        assertTrue(avatarUrl != null && avatarUrl.startsWith("/uploads/"),
                "profile 应带回刚上传的头像路径");
    }

    @Test
    @Order(4)
    void 再次上传_旧文件被删() throws Exception {
        // 先取当前旧文件名
        MvcResult before = mockMvc.perform(get("/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String oldUrl = om.readTree(before.getResponse().getContentAsString())
                .get("data").get("avatarUrl").asText();
        File oldFile = new File(uploadDir, oldUrl.replace("/uploads/", ""));
        assertTrue(oldFile.exists(), "上传前旧文件应在");

        // 再传一张
        MockMultipartFile file = new MockMultipartFile(
                "file", "head2.png", "image/png", new byte[]{9, 9, 9});
        MvcResult result = mockMvc.perform(multipart("/user/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        String newUrl = body.get("data").asText();
        assertTrue(!newUrl.equals(oldUrl), "新上传应生成不同文件名");

        // 旧文件应被删除（UserService 在更新 DB 前删旧图），避免磁盘无限膨胀
        assertTrue(!oldFile.exists(), "旧头像文件应被删除：" + oldFile.getAbsolutePath());
    }

    @Test
    @Order(5)
    void 上传非图片类型_被拒() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.txt", "text/plain", "hello".getBytes());
        MvcResult result = mockMvc.perform(multipart("/user/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(4002, om.readTree(result.getResponse().getContentAsString())
                .get("code").asInt(), "非图片类型应被拒（4002）");
    }

    @Test
    @Order(6)
    void 上传空文件_被拒() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        MvcResult result = mockMvc.perform(multipart("/user/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(4001, om.readTree(result.getResponse().getContentAsString())
                .get("code").asInt(), "空文件应被拒（4001）");
    }
}
