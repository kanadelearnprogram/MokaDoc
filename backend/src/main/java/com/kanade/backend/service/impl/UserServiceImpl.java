package com.kanade.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.kanade.backend.dto.user.LoginVO;
import com.kanade.backend.dto.user.UserLoginRequest;
import com.kanade.backend.dto.user.UserQueryRequest;
import com.kanade.backend.dto.user.UserRegisterRequest;
import com.kanade.backend.dto.user.UserVO;
import com.kanade.backend.entity.User;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.UserMapper;
import com.kanade.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户表 服务层实现。
 *
 * @author kanade
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，用于密码加密混淆
     */
    private static final String SALT = "kanade_mokadoc";

    @Override
    public Long userRegister(UserRegisterRequest registerRequest) {
        // 1. 校验参数
        String username = registerRequest.getUsername();
        String email = registerRequest.getEmail();
        String password = registerRequest.getPassword();
        String nickname = registerRequest.getNickname();

        if (StrUtil.hasBlank(username, email, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名、邮箱和密码不能为空");
        }
        if (username.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度过短（至少4位）");
        }
        if (password.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短（至少6位）");
        }

        // 2. 查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("username", username);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }

        // 检查邮箱是否已存在
        queryWrapper = new QueryWrapper();
        queryWrapper.eq("email", email);
        count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
        }

        // 3. 加密密码
        String encryptPassword = getEncryptPassword(password);

        // 4. 创建用户，插入数据库
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encryptPassword);
        user.setNickname(StrUtil.isNotBlank(nickname) ? nickname : username);
        user.setAvatar("");
        //user.setUserRole("user"); // 默认角色为普通用户
        user.setStatus(1);
        user.setRegisterTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleteFlag(0);

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
        }

        log.info("用户注册成功: username={}, email={}", username, email);
        return user.getId();
    }

    @Override
    public LoginVO userLogin(UserLoginRequest loginRequest) {
        // 1. 校验参数
        String account = loginRequest.getAccount();
        String password = loginRequest.getPassword();

        if (StrUtil.hasBlank(account, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号和密码不能为空");
        }

        // 2. 加密密码
        String encryptPassword = getEncryptPassword(password);
        System.out.println(encryptPassword);
        // 3. 查询用户是否存在（支持用户名或邮箱登录）
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.where("(username = ? OR email = ?) AND password = ?", account, account, encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        System.out.println(user);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 4. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用");
        }

        // 5. 记录用户的登录态
        HttpServletRequest request = getRequest();
        request.getSession().setAttribute("USER_LOGIN_STATE", user);
        log.info("用户登录成功: userId={}, account={}", user.getId(), account);

        // 6. 返回脱敏的用户信息
        UserVO userVO = getUserVO(user);
        return LoginVO.builder()
                .user(userVO)
                .token(request.getSession().getId())
                .build();
    }

    @Override
    public UserVO getUserVO(UserQueryRequest queryRequest) {
        if (queryRequest == null || queryRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        User user = this.getById(queryRequest.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return getUserVO(user);
    }

    @Override
    public UserVO getCurrentUserVO() {
        HttpServletRequest request = getRequest();
        User currentUser = getLoginUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        return getUserVO(currentUser);
    }

    @Override
    public void userLogout() {
        HttpServletRequest request = getRequest();
        Object userObj = request.getSession().getAttribute("USER_LOGIN_STATE");
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        request.getSession().removeAttribute("USER_LOGIN_STATE");
        log.info("用户注销成功");
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        try {
            // 先判断用户是否登录
            Object userObj = request.getSession().getAttribute("USER_LOGIN_STATE");
            User currentUser = (User) userObj;
            if (currentUser == null || currentUser.getId() == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            // 从数据库查询当前用户信息（保证数据最新）
            long userId = currentUser.getId();
            currentUser = this.getById(userId);
            if (currentUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            return currentUser;
        } catch (Exception e) {
            log.warn("获取当前用户失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }

    @Override
    public LoginVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginVO loginUserVO = new LoginVO();
        UserVO userVO = getUserVO(user);
        loginUserVO.setUser(userVO);
        return loginUserVO;
    }

    /**
     * 获取HttpServletRequest
     *
     * @return HttpServletRequest
     */
    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户实体
     * @return 用户VO
     */
    private UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 加密密码
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    private String getEncryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((password + SALT).getBytes(StandardCharsets.UTF_8));
    }
}
