package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lunar.lunar_backend.dto.LicenceActivateRequest;
import com.lunar.lunar_backend.dto.LicenceActivateResponse;
import com.lunar.lunar_backend.dto.LicenceAdminResponse;
import com.lunar.lunar_backend.dto.LicenceGenerateRequest;
import com.lunar.lunar_backend.dto.LicenceResponse;
import com.lunar.lunar_backend.dto.LicenceUpdateRequest;
import com.lunar.lunar_backend.dto.LicenceVipStatus;
import com.lunar.lunar_backend.entity.Licence;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.LicenceMapper;
import com.lunar.lunar_backend.service.LicenceService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LicenceServiceImpl extends ServiceImpl<LicenceMapper, Licence> implements LicenceService {

    private static final int STATUS_UNUSED = 1;
    private static final int STATUS_USED = 2;
    private static final int DEFAULT_COUNT = 1;
    private static final int MAX_COUNT = 100;
    private static final int VIP_DURATION_DAYS = 30;
    private static final int CODE_GROUPS = 4;
    private static final int CODE_GROUP_LENGTH = 4;
    private static final int MAX_GENERATE_RETRY = 10;
    private static final String DEFAULT_LICENCE_TYPE = "vip";
    private static final String CODE_PREFIX = "LUNAR";
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public List<LicenceResponse> generate(LicenceGenerateRequest request) {
        int count = validCount(request == null ? null : request.count());
        String licenceType = defaultText(request == null ? null : request.licenceType(), DEFAULT_LICENCE_TYPE);
        String remark = defaultText(request == null ? null : request.remark(), "");

        List<LicenceResponse> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Licence licence = new Licence();
            licence.setLicenceCode(nextUniqueCode());
            licence.setLicenceType(licenceType);
            // 未激活的码永不过期，expireTime 在激活时才写入
            licence.setStatus(STATUS_UNUSED);
            licence.setRemark(remark);
            save(licence);
            result.add(toResponse(licence));
        }
        return result;
    }

    @Override
    public LicenceActivateResponse activate(Long userId, LicenceActivateRequest request) {
        if (userId == null || request == null || !StringUtils.hasText(request.licenceCode())) {
            throw new ApiException(4000, "激活码不能为空");
        }
        String code = request.licenceCode().trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        boolean activated = update(new LambdaUpdateWrapper<Licence>()
                .set(Licence::getUserId, userId)
                .set(Licence::getStatus, STATUS_USED)
                // 激活时才确定会员到期时间：当前时间 + 30 天
                .set(Licence::getExpireTime, now.plusDays(VIP_DURATION_DAYS))
                .eq(Licence::getLicenceCode, code)
                .eq(Licence::getStatus, STATUS_UNUSED)
                .isNull(Licence::getUserId)
                .eq(Licence::getIsDelete, 0));
        if (!activated) {
            throw new ApiException(4000, "激活码无效或已被使用");
        }
        LicenceVipStatus vipStatus = currentVipStatus(userId);
        return new LicenceActivateResponse(vipStatus.isVip(), vipStatus.vipExpireTime(), vipStatus.licenceType());
    }

    @Override
    public LicenceVipStatus currentVipStatus(Long userId) {
        if (userId == null) {
            return new LicenceVipStatus(false, "", "");
        }
        LocalDateTime now = LocalDateTime.now();
        List<Licence> licences = list(new LambdaQueryWrapper<Licence>()
                .eq(Licence::getUserId, userId)
                .eq(Licence::getStatus, STATUS_USED));
        LocalDateTime latestExpireTime = null;
        String licenceType = "";
        for (Licence licence : licences) {
            LocalDateTime memberExpireTime = licence.getExpireTime();
            if (memberExpireTime == null || !memberExpireTime.isAfter(now)) {
                continue;
            }
            if (latestExpireTime == null || memberExpireTime.isAfter(latestExpireTime)) {
                latestExpireTime = memberExpireTime;
                licenceType = licence.getLicenceType();
            }
        }
        if (latestExpireTime == null) {
            return new LicenceVipStatus(false, "", "");
        }
        return new LicenceVipStatus(true, DATE_TIME_FORMATTER.format(latestExpireTime), licenceType);
    }

    @Override
    public IPage<LicenceAdminResponse> listLicences(Integer page, Integer pageSize, Integer status) {
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<Licence> wrapper = new LambdaQueryWrapper<Licence>()
                .orderByDesc(Licence::getCreateTime);
        if (status != null) {
            wrapper.eq(Licence::getStatus, status);
        }
        return page(new Page<>(p, ps), wrapper).convert(this::toAdminResponse);
    }

    @Override
    public void updateLicence(Long id, LicenceUpdateRequest request) {
        if (id == null) {
            throw new ApiException(4000, "ID 不能为空");
        }
        if (getById(id) == null) {
            throw new ApiException(4040, "激活码不存在");
        }
        LambdaUpdateWrapper<Licence> wrapper = new LambdaUpdateWrapper<Licence>()
                .eq(Licence::getId, id);
        if (StringUtils.hasText(request.licenceType())) {
            wrapper.set(Licence::getLicenceType, request.licenceType().trim());
        }
        if (request.remark() != null) {
            wrapper.set(Licence::getRemark, request.remark().trim());
        }
        update(wrapper);
    }

    @Override
    public void deleteLicence(Long id) {
        if (id == null) {
            throw new ApiException(4000, "ID 不能为空");
        }
        if (!removeById(id)) {
            throw new ApiException(4040, "激活码不存在");
        }
    }

    private String nextUniqueCode() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String code = nextCode();
            if (!existsCode(code)) {
                return code;
            }
        }
        throw new ApiException(5000, "激活码生成失败，请重试");
    }

    private String nextCode() {
        StringBuilder builder = new StringBuilder(CODE_PREFIX);
        for (int group = 0; group < CODE_GROUPS; group++) {
            builder.append("-");
            for (int index = 0; index < CODE_GROUP_LENGTH; index++) {
                builder.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
            }
        }
        return builder.toString();
    }

    private boolean existsCode(String code) {
        return count(new LambdaQueryWrapper<Licence>()
                .eq(Licence::getLicenceCode, code)) > 0;
    }

    private int validCount(Integer count) {
        int value = count == null ? DEFAULT_COUNT : count;
        if (value < 1 || value > MAX_COUNT) {
            throw new ApiException(4000, "生成数量需为 1 到 100");
        }
        return value;
    }

    private LicenceResponse toResponse(Licence licence) {
        return new LicenceResponse(
                licence.getId(),
                licence.getLicenceCode(),
                licence.getLicenceType(),
                licence.getExpireTime() == null ? "" : DATE_TIME_FORMATTER.format(licence.getExpireTime()),
                licence.getStatus(),
                licence.getRemark(),
                licence.getCreateTime() == null ? "" : DATE_TIME_FORMATTER.format(licence.getCreateTime())
        );
    }

    private LicenceAdminResponse toAdminResponse(Licence licence) {
        return new LicenceAdminResponse(
                licence.getId(),
                licence.getLicenceCode(),
                licence.getLicenceType(),
                licence.getExpireTime() == null ? "" : DATE_TIME_FORMATTER.format(licence.getExpireTime()),
                licence.getStatus(),
                licence.getRemark(),
                licence.getUserId(),
                licence.getCreateTime() == null ? "" : DATE_TIME_FORMATTER.format(licence.getCreateTime())
        );
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
