package com.lunar.lunar_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.dto.AiAnalyzeRequest;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.service.AiService;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${deepseek.read-timeout:90000}")
    private int readTimeout;

    @PostConstruct
    public void logDeepSeekConfig() {
        log.info(
                "DeepSeek config loaded: baseUrl={}, model={}, apiKeyPresent={}, connectTimeout={}, readTimeout={}",
                baseUrl,
                model,
                StringUtils.hasText(apiKey),
                connectTimeout,
                readTimeout
        );
    }

    @Override
    public String analyze(AiAnalyzeRequest request) {
        if (request == null || !StringUtils.hasText(request.resultJson())) {
            throw new ApiException(ErrorCode.CHART_RESULT_EMPTY);
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(ErrorCode.AI_CONFIG_MISSING);
        }

        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setReadTimeout(readTimeout);

            RestClient restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(requestFactory)
                    .build();

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "stream", false,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(request.resultJson()))
                    )
            );

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode contentNode = objectMapper.readTree(responseBody)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");
            if (!contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
                throw new ApiException(ErrorCode.AI_ANALYZE_FAILED);
            }
            return contentNode.asText();
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("DeepSeek analyze failed", exception);
            throw new ApiException(ErrorCode.AI_ANALYZE_FAILED, "AI分析失败：" + exception.getMessage());
        }
    }

    private String systemPrompt() {
        return "你是专业八字命理分析师。请基于用户排盘 JSON 做结构化分析，语言清晰、温和、克制。"
                + "不要编造 JSON 中没有的信息，不要给医疗、法律、投资等高风险决策建议。";
    }

    private String userPrompt(String resultJson) {
        return "请分析下面这个四柱八字排盘结果 JSON，输出：1. 基础格局 2. 五行强弱 3. 性格倾向 "
                + "4. 事业财运 5. 感情家庭 6. 流年提醒。JSON：\n" + resultJson;
    }
}
