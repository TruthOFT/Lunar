package com.lunar.lunar_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lunar.lunar_backend.dto.RecordResponse;
import com.lunar.lunar_backend.dto.RecordSaveRequest;
import com.lunar.lunar_backend.entity.ChartRecord;
import java.util.List;

public interface RecordService extends IService<ChartRecord> {

    RecordResponse saveRecord(Long userId, RecordSaveRequest request);

    List<RecordResponse> listRecords(Long userId);
}
