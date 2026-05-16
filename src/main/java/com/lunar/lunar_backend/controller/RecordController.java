package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.common.AuthContext;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.dto.RecordResponse;
import com.lunar.lunar_backend.dto.RecordSaveRequest;
import com.lunar.lunar_backend.service.RecordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/records")
public class RecordController {

    @Resource
    private RecordService recordService;

    @PostMapping
    public ApiResponse<RecordResponse> save(@RequestBody RecordSaveRequest requestBody, HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        return ApiResponse.success(recordService.saveRecord(authUser.userId(), requestBody));
    }

    @GetMapping
    public ApiResponse<List<RecordResponse>> list(HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        return ApiResponse.success(recordService.listRecords(authUser.userId()));
    }
}
