package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunar.lunar_backend.dto.RecordResponse;
import com.lunar.lunar_backend.dto.RecordSaveRequest;
import com.lunar.lunar_backend.entity.ChartRecord;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.ChartRecordMapper;
import com.lunar.lunar_backend.service.RecordService;
import jakarta.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecordServiceImpl extends ServiceImpl<ChartRecordMapper, ChartRecord> implements RecordService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CACHE_KEY_PREFIX = "records:user:";
    private static final long CACHE_TTL_MINUTES = 30;
    private static final TypeReference<List<RecordResponse>> LIST_TYPE = new TypeReference<>() {};

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RecordCacheService recordCacheService;

    @Override
    public RecordResponse saveRecord(Long userId, RecordSaveRequest request) {
        if (!StringUtils.hasText(request.resultJson())) {
            throw new ApiException(ErrorCode.CHART_RESULT_EMPTY);
        }
        ChartRecord record = new ChartRecord();
        record.setUserId(userId);
        record.setTitle(defaultText(request.title(), "排盘记录"));
        record.setChartName(defaultText(request.chartName(), "未命名"));
        record.setGender(defaultText(request.gender(), ""));
        record.setBirthTime(defaultText(request.birthTime(), ""));
        record.setResultJson(request.resultJson());
        save(record);
        recordCacheService.warmCache(userId);
        return toResponse(record);
    }

    @Override
    public List<RecordResponse> listRecords(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json != null) {
            try {
                return objectMapper.readValue(json, LIST_TYPE);
            } catch (JsonProcessingException e) {
                stringRedisTemplate.delete(cacheKey);
            }
        }
        List<RecordResponse> records = list(new LambdaQueryWrapper<ChartRecord>()
                        .eq(ChartRecord::getUserId, userId)
                        .orderByDesc(ChartRecord::getCreateTime)
                        .orderByDesc(ChartRecord::getId))
                .stream()
                .map(this::toResponse)
                .toList();
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(records),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException ignored) {
        }
        return records;
    }

    @Override
    public void deleteRecord(Long userId, Long recordId) {
        ChartRecord record = getById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        removeById(recordId);
        recordCacheService.warmCache(userId);
    }

    private RecordResponse toResponse(ChartRecord record) {
        return new RecordResponse(
                record.getId(),
                record.getTitle(),
                record.getChartName(),
                record.getGender(),
                record.getBirthTime(),
                record.getResultJson(),
                record.getCreateTime() == null ? "" : DATE_TIME_FORMATTER.format(record.getCreateTime())
        );
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
