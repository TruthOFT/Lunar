package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lunar.lunar_backend.dto.LicenceActivateRequest;
import com.lunar.lunar_backend.dto.LicenceActivateResponse;
import com.lunar.lunar_backend.dto.LicenceGenerateRequest;
import com.lunar.lunar_backend.dto.LicenceResponse;
import com.lunar.lunar_backend.dto.LicenceVipStatus;
import com.lunar.lunar_backend.entity.Licence;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.LicenceMapper;
import com.lunar.lunar_backend.service.LicenceService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final int DEFAULT_DURATION_DAYS = 30;
    private static final int MAX_DURATION_DAYS = 3650;
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
        int durationDays = validDurationDays(request == null ? null : request.durationDays());
        LocalDateTime expireTime = parseExpireTime(request == null ? null : request.expireTime());
        String licenceType = defaultText(request == null ? null : request.licenceType(), DEFAULT_LICENCE_TYPE);
        String remark = defaultText(request == null ? null : request.remark(), "");

        List<LicenceResponse> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Licence licence = new Licence();
            licence.setLicenceCode(nextUniqueCode());
            licence.setLicenceType(licenceType);
            licence.setDurationDays(durationDays);
            licence.setExpireTime(expireTime);
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
            throw new ApiException(4000, "Licence code cannot be empty");
        }
        String code = request.licenceCode().trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        boolean activated = update(new LambdaUpdateWrapper<Licence>()
                .set(Licence::getUserId, userId)
                .set(Licence::getStatus, STATUS_USED)
                .set(Licence::getUsedTime, now)
                .eq(Licence::getLicenceCode, code)
                .eq(Licence::getStatus, STATUS_UNUSED)
                .isNull(Licence::getUserId)
                .eq(Licence::getIsDelete, 0)
                .and(wrapper -> wrapper.isNull(Licence::getExpireTime)
                        .or()
                        .gt(Licence::getExpireTime, now)));
        if (!activated) {
            throw new ApiException(4000, "Licence code invalid or already used");
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
            if (licence.getUsedTime() == null || licence.getDurationDays() == null) {
                continue;
            }
            LocalDateTime memberExpireTime = licence.getUsedTime().plusDays(licence.getDurationDays());
            if (!memberExpireTime.isAfter(now)) {
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

    private String nextUniqueCode() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String code = nextCode();
            if (!existsCode(code)) {
                return code;
            }
        }
        throw new ApiException(5000, "Licence code generate failed, please retry");
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
            throw new ApiException(4000, "Generate count must be between 1 and 100");
        }
        return value;
    }

    private int validDurationDays(Integer durationDays) {
        int value = durationDays == null ? DEFAULT_DURATION_DAYS : durationDays;
        if (value < 1 || value > MAX_DURATION_DAYS) {
            throw new ApiException(4000, "Duration days must be between 1 and 3650");
        }
        return value;
    }

    private LocalDateTime parseExpireTime(String expireTime) {
        if (!StringUtils.hasText(expireTime)) {
            return null;
        }
        try {
            return LocalDateTime.parse(expireTime.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new ApiException(4000, "Expire time format must be yyyy-MM-dd HH:mm:ss");
        }
    }

    private LicenceResponse toResponse(Licence licence) {
        return new LicenceResponse(
                licence.getId(),
                licence.getLicenceCode(),
                licence.getLicenceType(),
                licence.getDurationDays(),
                licence.getExpireTime() == null ? "" : DATE_TIME_FORMATTER.format(licence.getExpireTime()),
                licence.getStatus(),
                licence.getRemark(),
                licence.getCreateTime() == null ? "" : DATE_TIME_FORMATTER.format(licence.getCreateTime())
        );
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
