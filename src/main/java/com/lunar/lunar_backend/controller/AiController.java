package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.common.AuthContext;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.dto.AiAnalyzeRequest;
import com.lunar.lunar_backend.service.AiService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
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
    public ApiResponse<String> analyze(@RequestBody AiAnalyzeRequest request, HttpServletRequest httpRequest) {
        AuthUser authUser = (AuthUser) httpRequest.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        return ApiResponse.success(aiService.analyze(authUser.userId(), request));
    }

    @PostMapping("/analyze/stream")
    public SseEmitter analyzeStream(@RequestBody AiAnalyzeRequest request, HttpServletRequest httpRequest) {
        AuthUser authUser = (AuthUser) httpRequest.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        Long userId = authUser.userId();
        SseEmitter emitter = new SseEmitter(300_000L);
        CompletableFuture.runAsync(() -> aiService.analyzeStream(userId, request, emitter));
        return emitter;
    }
}
