package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lunar.lunar_backend.dto.AuthResponse;
import com.lunar.lunar_backend.dto.LicenceVipStatus;
import com.lunar.lunar_backend.dto.LoginRequest;
import com.lunar.lunar_backend.dto.RegisterRequest;
import com.lunar.lunar_backend.dto.UserAdminResponse;
import com.lunar.lunar_backend.dto.UserResponse;
import com.lunar.lunar_backend.entity.LunarUser;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.LunarUserMapper;
import com.lunar.lunar_backend.service.AuthService;
import com.lunar.lunar_backend.service.LicenceService;
import com.lunar.lunar_backend.util.JwtUtil;
import com.lunar.lunar_backend.util.Md5Util;
import jakarta.annotation.Resource;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl extends ServiceImpl<LunarUserMapper, LunarUser> implements AuthService {

    private static final String PASSWORD_SALT = "lunar_password_salt_2026";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private LicenceService licenceService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        validateAccountAndPassword(request.account(), request.password());
        LunarUser exists = getByAccount(request.account());
        if (exists != null) {
            throw new ApiException(ErrorCode.ACCOUNT_EXISTS);
        }

        LunarUser user = new LunarUser();
        user.setAccount(request.account().trim());
        user.setPassword(encryptPassword(request.password()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : request.account().trim());
        user.setTokenVersion(1);
        save(user);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        validateAccountAndPassword(request.account(), request.password());
        LunarUser user = getByAccount(request.account());
        if (user == null || !encryptPassword(request.password()).equals(user.getPassword())) {
            throw new ApiException(ErrorCode.ACCOUNT_PASSWORD_ERROR);
        }
        return buildAuthResponse(user);
    }

    @Override
    public UserResponse currentUser(Long userId) {
        LunarUser user = getById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.LOGIN_INVALID);
        }
        return toUserResponse(user);
    }

    @Override
    public LunarUser requireValidTokenUser(Long userId, Integer tokenVersion) {
        LunarUser user = getById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.LOGIN_INVALID);
        }
        if (!tokenVersion.equals(user.getTokenVersion())) {
            throw new ApiException(ErrorCode.LOGIN_INVALID);
        }
        return user;
    }

    @Override
    public IPage<UserAdminResponse> listUsers(Integer page, Integer pageSize, String keyword) {
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<LunarUser> wrapper = new LambdaQueryWrapper<LunarUser>()
                .orderByDesc(LunarUser::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(LunarUser::getAccount, keyword).or().like(LunarUser::getNickname, keyword));
        }
        return page(new Page<>(p, ps), wrapper).convert(this::toAdminUserResponse);
    }

    @Override
    public void updateUserRole(Long id, Integer role) {
        if (id == null) {
            throw new ApiException(4000, "ID 不能为空");
        }
        if (role == null || (role != 0 && role != 1)) {
            throw new ApiException(4000, "角色只能为 0 或 1");
        }
        if (getById(id) == null) {
            throw new ApiException(4040, "用户不存在");
        }
        update(new LambdaUpdateWrapper<LunarUser>()
                .eq(LunarUser::getId, id)
                .set(LunarUser::getRole, role));
    }

    @Override
    public void deleteUser(Long id) {
        if (id == null) {
            throw new ApiException(4000, "ID 不能为空");
        }
        if (!removeById(id)) {
            throw new ApiException(4040, "用户不存在");
        }
    }

    private UserAdminResponse toAdminUserResponse(LunarUser user) {
        String createTime = user.getCreateTime() != null
                ? user.getCreateTime().format(DATE_TIME_FORMATTER) : "";
        return new UserAdminResponse(user.getId(), user.getAccount(), user.getNickname(), user.getRole(), createTime);
    }

    private LunarUser getByAccount(String account) {
        return getOne(new LambdaQueryWrapper<LunarUser>()
                .eq(LunarUser::getAccount, account.trim())
                .last("limit 1"));
    }

    private AuthResponse buildAuthResponse(LunarUser user) {
        String token = JwtUtil.createToken(user.getId(), user.getAccount(), user.getTokenVersion());
        return new AuthResponse(token, toUserResponse(user));
    }

    private UserResponse toUserResponse(LunarUser user) {
        LicenceVipStatus vipStatus = licenceService.currentVipStatus(user.getId());
        return new UserResponse(
                user.getId(),
                user.getAccount(),
                user.getNickname(),
                vipStatus.isVip(),
                vipStatus.vipExpireTime(),
                vipStatus.licenceType()
        );
    }

    private String encryptPassword(String password) {
        return Md5Util.md5(password + PASSWORD_SALT);
    }

    private void validateAccountAndPassword(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new ApiException(ErrorCode.ACCOUNT_PASSWORD_EMPTY);
        }
        if (account.trim().length() < 3 || account.trim().length() > 32) {
            throw new ApiException(ErrorCode.ACCOUNT_LENGTH_ERROR);
        }
        if (password.length() < 3 || password.length() > 64) {
            throw new ApiException(ErrorCode.PASSWORD_LENGTH_ERROR);
        }
    }
}
