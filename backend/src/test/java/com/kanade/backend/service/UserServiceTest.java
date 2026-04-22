package com.kanade.backend.service;

import com.kanade.backend.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务层测试类
 * 测试SysUserService的基本增删改查功能，验证数据库连接
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    @Autowired
    private UserService sysUserService;

    private Long testUserId;

    /**
     * 测试新增用户 - 验证数据库写入功能
     */
    @Test
    @Order(1)
    @DisplayName("测试新增用户")
    void testSave() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encrypted_password_123");
        user.setNickname("测试用户");
        user.setAvatar("https://example.com/avatar.jpg");
        user.setStatus(1);
        user.setRegisterTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleteFlag(0);

        boolean result = sysUserService.save(user);
        assertTrue(result, "用户保存应该成功");
        assertNotNull(user.getId(), "保存后用户ID不应为空");
        
        testUserId = user.getId();
        System.out.println("创建的用户ID: " + testUserId);
    }

    /**
     * 测试根据ID查询用户 - 验证数据库读取功能
     */
    @Test
    @Order(2)
    @DisplayName("测试根据ID查询用户")
    void testGetById() {
        // 先创建一个用户用于测试
        User user = new User();
        user.setUsername("testuser2");
        user.setEmail("test2@example.com");
        user.setPassword("password");
        user.setNickname("测试用户2");
        user.setStatus(1);
        user.setRegisterTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleteFlag(0);
        sysUserService.save(user);
        
        User found = sysUserService.getById(user.getId());
        assertNotNull(found, "查询结果不应为空");
        assertEquals("testuser2", found.getUsername(), "用户名应该匹配");
        
        // 清理测试数据
        sysUserService.removeById(user.getId());
        System.out.println("查询到的用户: " + found.getUsername());
    }

    /**
     * 测试查询所有用户 - 验证列表查询功能
     */
    @Test
    @Order(3)
    @DisplayName("测试查询所有用户")
    void testList() {
        List<User> userList = sysUserService.list();
        assertNotNull(userList, "用户列表不应为空");
        
        System.out.println("用户总数: " + userList.size());
    }

    /**
     * 测试更新用户 - 验证数据更新功能
     */
    @Test
    @Order(4)
    @DisplayName("测试更新用户")
    void testUpdate() {
        // 先创建一个用户
        User user = new User();
        user.setUsername("testuser3");
        user.setEmail("test3@example.com");
        user.setPassword("password");
        user.setNickname("原始昵称");
        user.setStatus(1);
        user.setRegisterTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleteFlag(0);
        sysUserService.save(user);
        
        // 更新用户
        user.setNickname("更新后的昵称");
        user.setEmail("updated@example.com");
        user.setUpdateTime(LocalDateTime.now());
        
        boolean result = sysUserService.updateById(user);
        assertTrue(result, "用户更新应该成功");
        
        User updatedUser = sysUserService.getById(user.getId());
        assertEquals("更新后的昵称", updatedUser.getNickname(), "昵称应该已更新");
        
        // 清理测试数据
        sysUserService.removeById(user.getId());
        System.out.println("用户已更新");
    }

    /**
     * 测试删除用户 - 验证逻辑删除功能
     */
    @Test
    @Order(5)
    @DisplayName("测试删除用户")
    void testRemove() {
        // 先创建一个用户
        User user = new User();
        user.setUsername("testuser4");
        user.setEmail("test4@example.com");
        user.setPassword("password");
        user.setNickname("待删除用户");
        user.setStatus(1);
        user.setRegisterTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleteFlag(0);
        sysUserService.save(user);
        
        Long userId = user.getId();
        
        boolean result = sysUserService.removeById(userId);
        assertTrue(result, "用户删除应该成功");
        
        User deletedUser = sysUserService.getById(userId);
        assertNull(deletedUser, "删除后查询应该返回null（逻辑删除）");
        
        System.out.println("用户已删除");
    }

    /**
     * 测试统计功能 - 验证记录计数
     */
    @Test
    @Order(6)
    @DisplayName("测试统计用户数量")
    void testCount() {
        long count = sysUserService.count();
        assertTrue(count >= 0, "用户数量应该大于等于0");
        System.out.println("当前用户总数: " + count);
    }
}
