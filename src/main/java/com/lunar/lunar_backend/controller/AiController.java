package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.dto.AiAnalyzeRequest;
import com.lunar.lunar_backend.service.AiService;
import jakarta.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @PostMapping("/analyze")
    public ApiResponse<String> analyze(@RequestBody AiAnalyzeRequest request) {
        return ApiResponse.success(aiService.analyze(request));
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(@RequestBody AiAnalyzeRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> aiService.analyzeStream(request, emitter));
        return emitter;
    }
}
