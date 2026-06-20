package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunar.lunar_backend.dto.RecordResponse;
import com.lunar.lunar_backend.entity.ChartRecord;
import com.lunar.lunar_backend.mapper.ChartRecordMapper;
import jakarta.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RecordCacheService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CACHE_KEY_PREFIX = "records:user:";
    private static final long CACHE_TTL_MINUTES = 30;

    @Resource
    private ChartRecordMapper chartRecordMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Async
    public void warmCache(Long userId) {
        List<RecordResponse> records = chartRecordMapper.selectList(
                        new LambdaQueryWrapper<ChartRecord>()
                                .eq(ChartRecord::getUserId, userId)
                                .orderByDesc(ChartRecord::getCreateTime)
                                .orderByDesc(ChartRecord::getId))
                .stream()
                .map(this::toResponse)
                .toList();
        try {
            stringRedisTemplate.opsForValue().set(CACHE_KEY_PREFIX + userId,
                    objectMapper.writeValueAsString(records), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException ignored) {
        }
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
}
