package com.lunar.lunar_backend.service;

import com.lunar.lunar_backend.dto.AiAnalyzeRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {

    String analyze(Long userId, AiAnalyzeRequest request);

    void analyzeStream(Long userId, AiAnalyzeRequest request, SseEmitter emitter);
}
