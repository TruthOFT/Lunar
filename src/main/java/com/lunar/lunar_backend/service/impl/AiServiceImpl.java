package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.dto.AiAnalyzeRequest;
import com.lunar.lunar_backend.entity.AiAnalysis;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.AiAnalysisMapper;
import com.lunar.lunar_backend.dto.LicenceVipStatus;
import com.lunar.lunar_backend.entity.LunarUser;
import com.lunar.lunar_backend.mapper.LunarUserMapper;
import com.lunar.lunar_backend.service.AiService;
import com.lunar.lunar_backend.service.LicenceService;
import jakarta.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Resource
    private AiAnalysisMapper aiAnalysisMapper;

    @Resource
    private LunarUserMapper lunarUserMapper;

    @Resource
    private LicenceService licenceService;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, String> CATEGORY_PROMPTS = Map.ofEntries(
            Map.entry("总览", AiPrompts.OVERVIEW),
            Map.entry("财运", AiPrompts.WEALTH),
            Map.entry("事业", AiPrompts.CAREER),
            Map.entry("学业", AiPrompts.STUDY),
            Map.entry("姻缘", AiPrompts.MARRIAGE),
            Map.entry("运势", AiPrompts.FORTUNE),
            Map.entry("流年", AiPrompts.YEAR_FORTUNE),
            Map.entry("健康", AiPrompts.HEALTH)
    );

    @Override
    public String analyze(Long userId, AiAnalyzeRequest request) {
        if (request == null || !StringUtils.hasText(request.resultJson())) {
            throw new ApiException(ErrorCode.CHART_RESULT_EMPTY);
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(ErrorCode.AI_CONFIG_MISSING);
        }

        String category = normalizeCategory(request.category());
        boolean isVip = checkAiPermission(userId);

        if (request.recordId() != null && !Boolean.TRUE.equals(request.force())) {
            AiAnalysis cached = getCached(request.recordId(), category);
            if (cached != null) return cached.getContent();
        }

        // 非VIP，发请求前立即扣次数
        if (!isVip) {
            deductAiCount(userId);
        }

        try {
            String prompt = buildUserPrompt(category, request.systemPrompt(), request.resultJson());
            String sysPrompt = CATEGORY_PROMPTS.get(category);
            String body = buildRequestBody(sysPrompt, prompt, false);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .post(RequestBody.create(body, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new ApiException(ErrorCode.AI_ANALYZE_FAILED, "DeepSeek返回错误: " + response.code());
                }
                String responseBody = response.body().string();
                JsonNode root = MAPPER.readTree(responseBody);
                String result = root.path("choices").path(0).path("message").path("content").asText();
                if (!StringUtils.hasText(result)) {
                    throw new ApiException(ErrorCode.AI_ANALYZE_FAILED);
                }
                if (request.recordId() != null) saveCache(request.recordId(), category, result);
                return result;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek analyze failed", e);
            throw new ApiException(ErrorCode.AI_ANALYZE_FAILED, "AI分析失败：" + e.getMessage());
        }
    }

    @Override
    public void analyzeStream(Long userId, AiAnalyzeRequest request, SseEmitter emitter) {
        sendEvent(emitter, "ready", "");
        if (request == null || !StringUtils.hasText(request.resultJson())) {
            completeWithError(emitter, new ApiException(ErrorCode.CHART_RESULT_EMPTY));
            return;
        }
        if (!StringUtils.hasText(apiKey)) {
            completeWithError(emitter, new ApiException(ErrorCode.AI_CONFIG_MISSING));
            return;
        }

        boolean isVip;
        try {
            isVip = checkAiPermission(userId);
        } catch (ApiException e) {
            completeWithError(emitter, e);
            return;
        }

        String category = normalizeCategory(request.category());

        if (request.recordId() != null && !Boolean.TRUE.equals(request.force())) {
            AiAnalysis cached = getCached(request.recordId(), category);
            if (cached != null) {
                try {
                    String content = cached.getContent();
                    int chunkSize = 200;
                    for (int i = 0; i < content.length(); i += chunkSize) {
                        sendEvent(emitter, "message", content.substring(i, Math.min(i + chunkSize, content.length())));
                    }
                    sendEvent(emitter, "done", "");
                    emitter.complete();
                } catch (Exception e) {
                    completeWithError(emitter, e);
                }
                return;
            }
        }

        // 非VIP，发请求前立即扣次数
        if (!isVip) {
            deductAiCount(userId);
        }

        try {
            String prompt = buildUserPrompt(category, request.systemPrompt(), request.resultJson());
            String sysPrompt = CATEGORY_PROMPTS.get(category);
            String body = buildRequestBody(sysPrompt, prompt, true);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .post(RequestBody.create(body, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            StringBuilder accumulated = new StringBuilder();
            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    completeWithError(emitter, new ApiException(ErrorCode.AI_ANALYZE_FAILED, "DeepSeek返回错误: " + response.code()));
                    return;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;
                            try {
                                JsonNode node = MAPPER.readTree(data);
                                String chunk = node.path("choices").path(0).path("delta").path("content").asText(null);
                                if (StringUtils.hasText(chunk)) {
                                    sendEvent(emitter, "message", chunk);
                                    accumulated.append(chunk);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            sendEvent(emitter, "done", "");
            emitter.complete();
            if (request.recordId() != null && !accumulated.isEmpty()) {
                saveCache(request.recordId(), category, accumulated.toString());
            }
        } catch (Exception e) {
            log.error("DeepSeek stream failed", e);
            completeWithError(emitter, e);
        }
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data == null ? "" : data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void completeWithError(SseEmitter emitter, Exception e) {
        try {
            sendEvent(emitter, "error", e.getMessage());
        } catch (Exception ignored) {
        }
        try {
            emitter.completeWithError(e);
        } catch (Exception ignored) {
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt, boolean stream) throws Exception {
        var messages = new com.fasterxml.jackson.databind.node.ArrayNode(MAPPER.getNodeFactory());
        if (StringUtils.hasText(systemPrompt)) {
            var sysMsg = MAPPER.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }
        var userMsg = MAPPER.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        var root = MAPPER.createObjectNode();
        root.put("model", model);
        root.set("messages", messages);
        root.put("stream", stream);
        return MAPPER.writeValueAsString(root);
    }

    private String buildUserPrompt(String category, String systemPrompt, String resultJson) {
        return "请分析下面的四柱八字排盘结果：\n" + resultJson;
    }

    private boolean checkAiPermission(Long userId) {
        LicenceVipStatus vipStatus = licenceService.currentVipStatus(userId);
        if (Boolean.TRUE.equals(vipStatus.isVip())) return true;
        LunarUser user = lunarUserMapper.selectById(userId);
        if (user != null && user.getAiRemainCount() != null && user.getAiRemainCount() > 0) return false;
        throw new ApiException(4030, "VIP已过期或AI次数已用完，请激活卡密");
    }

    private void deductAiCount(Long userId) {
        lunarUserMapper.update(null,
                new LambdaUpdateWrapper<LunarUser>()
                        .eq(LunarUser::getId, userId)
                        .gt(LunarUser::getAiRemainCount, 0)
                        .setSql("aiRemainCount = aiRemainCount - 1")
        );
    }

    private String normalizeCategory(String category) {
        return StringUtils.hasText(category) ? category : "总览";
    }

    private AiAnalysis getCached(Long recordId, String category) {
        return aiAnalysisMapper.selectOne(
                new LambdaQueryWrapper<AiAnalysis>()
                        .eq(AiAnalysis::getRecordId, recordId)
                        .eq(AiAnalysis::getCategory, category)
        );
    }

    private void saveCache(Long recordId, String category, String content) {
        AiAnalysis existing = getCached(recordId, category);
        if (existing != null) {
            aiAnalysisMapper.update(null,
                    new LambdaUpdateWrapper<AiAnalysis>()
                            .eq(AiAnalysis::getRecordId, recordId)
                            .eq(AiAnalysis::getCategory, category)
                            .set(AiAnalysis::getContent, content)
            );
        } else {
            AiAnalysis entity = new AiAnalysis();
            entity.setRecordId(recordId);
            entity.setCategory(category);
            entity.setContent(content);
            aiAnalysisMapper.insert(entity);
        }
    }
}
