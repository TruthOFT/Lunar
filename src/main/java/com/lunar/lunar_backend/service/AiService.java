package com.lunar.lunar_backend.service;

import com.lunar.lunar_backend.dto.AiAnalyzeRequest;

public interface AiService {

    String analyze(AiAnalyzeRequest request);
}
