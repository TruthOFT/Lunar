package com.lunar.lunar_backend.service;

import com.lunar.lunar_backend.dto.AiAnalyzeRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {

    String analyze(AiAnalyzeRequest request);

    void analyzeStream(AiAnalyzeRequest request, SseEmitter emitter);
}
