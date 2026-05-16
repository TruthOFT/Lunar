package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lunar.lunar_backend.dto.RecordResponse;
import com.lunar.lunar_backend.dto.RecordSaveRequest;
import com.lunar.lunar_backend.entity.ChartRecord;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.ChartRecordMapper;
import com.lunar.lunar_backend.service.RecordService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecordServiceImpl extends ServiceImpl<ChartRecordMapper, ChartRecord> implements RecordService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public RecordResponse saveRecord(Long userId, RecordSaveRequest request) {
        if (!StringUtils.hasText(request.resultJson())) {
            throw new ApiException(ErrorCode.CHART_RESULT_EMPTY);
        }
        ChartRecord record = new ChartRecord();
        record.setUserId(userId);
        record.setTitle(defaultText(request.title(), "\u6392\u76d8\u8bb0\u5f55"));
        record.setChartName(defaultText(request.chartName(), "\u672a\u547d\u540d"));
        record.setGender(defaultText(request.gender(), ""));
        record.setBirthTime(defaultText(request.birthTime(), ""));
        record.setResultJson(request.resultJson());
        save(record);
        return toResponse(record);
    }

    @Override
    public List<RecordResponse> listRecords(Long userId) {
        return list(new LambdaQueryWrapper<ChartRecord>()
                        .eq(ChartRecord::getUserId, userId)
                        .orderByDesc(ChartRecord::getCreateTime)
                        .orderByDesc(ChartRecord::getId))
                .stream()
                .map(this::toResponse)
                .toList();
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
