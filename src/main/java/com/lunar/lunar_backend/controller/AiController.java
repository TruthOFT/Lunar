package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.dto.AiAnalyzeRequest;
import com.lunar.lunar_backend.service.AiService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @PostMapping("/analyze")
    public ApiResponse<String> analyze(@RequestBody AiAnalyzeRequest request) {
        return ApiResponse.success(aiService.analyze(request));
    }
}
