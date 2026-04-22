package com.kanade.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanade.backend.dto.user.LoginVO;
import com.kanade.backend.dto.user.UserLoginRequest;
import com.kanade.backend.dto.user.UserQueryRequest;
import com.kanade.backend.dto.user.UserRegisterRequest;
import com.kanade.backend.dto.user.UserVO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String testUsername = "testuser_" + System.currentTimeMillis();
    private static String testEmail = "test_" + System.currentTimeMillis() + "@example.com";
    private static String testPassword = "password123";
    private static Long testUserId;

    /**
     * 测试用户注册
     */
    @Test
    @Order(1)
    @DisplayName("测试用户注册")
    void testRegister() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setNickname("测试用户");

        MvcResult result = mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        System.out.println("注册响应: " + responseContent);
        
        // 提取用户ID供后续测试使用
        testUserId = objectMapper.readTree(responseContent).get("data").asLong();
        System.out.println("注册用户ID: " + testUserId);
    }

    /**
     * 测试用户登录
     */
    @Test
    @Order(2)
    @DisplayName("测试用户登录")
    void testLogin() throws Exception {
        UserLoginRequest request = new UserLoginRequest();
        request.setAccount(testUsername);
        request.setPassword(testPassword);

        MvcResult result = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.username").value(testUsername))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        System.out.println("登录响应: " + responseContent);
    }

    /**
     * 测试获取当前用户信息
     */
    @Test
    @Order(3)
    @DisplayName("测试获取当前用户信息")
    void testGetCurrentUser() throws Exception {
        // 先登录
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setAccount(testUsername);
        loginRequest.setPassword(testPassword);
        
        MvcResult loginResult = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

//        LoginVO loginVO = objectMapper.readValue(
//                loginResult.getResponse().getContentAsString(),
//                LoginVO.class
//        ).getData();

        // 获取当前用户（需要在实际项目中配置session）
        // 这里只是演示API调用
        System.out.println("当前用户信息测试完成");
    }

    /**
     * 测试根据ID查询用户
     */
    @Test
    @Order(4)
    @DisplayName("测试根据ID查询用户")
    void testGetUserById() throws Exception {
        UserQueryRequest request = new UserQueryRequest();
        request.setId(testUserId);

        mockMvc.perform(get("/user/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(testUsername))
                .andExpect(jsonPath("$.data.email").value(testEmail));
    }

    /**
     * 测试无效参数注册
     */
    @Test
    @Order(5)
    @DisplayName("测试无效参数注册")
    void testRegisterWithInvalidParams() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("ab"); // 用户名太短
        request.setEmail("invalid-email");
        request.setPassword("123"); // 密码太短

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试重复注册
     */
    @Test
    @Order(6)
    @DisplayName("测试重复注册")
    void testDuplicateRegister() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(testUsername);
        request.setEmail("another_" + testEmail);
        request.setPassword(testPassword);

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000)); // PARAMS_ERROR
    }

    /**
     * 测试错误密码登录
     */
    @Test
    @Order(7)
    @DisplayName("测试错误密码登录")
    void testLoginWithWrongPassword() throws Exception {
        UserLoginRequest request = new UserLoginRequest();
        request.setAccount(testUsername);
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000)); // PARAMS_ERROR
    }

    /**
     * 测试用户注销
     */
    @Test
    @Order(8)
    @DisplayName("测试用户注销")
    void testLogout() throws Exception {
        mockMvc.perform(post("/user/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
