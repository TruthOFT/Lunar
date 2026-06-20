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
import com.lunar.lunar_backend.service.AiService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private AiAnalysisMapper aiAnalysisMapper;

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

        if (request.recordId() != null && !Boolean.TRUE.equals(request.force())) {
            AiAnalysis cached = getCached(request.recordId());
            if (cached != null) {
                return cached.getContent();
            }
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
                    "temperature", 0.5,
                    "top_p", 0.9,
                    "max_tokens", 5000,
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
            String content = contentNode.asText();
            if (request.recordId() != null) {
                saveCache(request.recordId(), content);
            }
            return content;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("DeepSeek analyze failed", exception);
            throw new ApiException(ErrorCode.AI_ANALYZE_FAILED, "AI分析失败：" + exception.getMessage());
        }
    }

    @Override
    public void analyzeStream(AiAnalyzeRequest request, SseEmitter emitter) {
        if (request == null || !StringUtils.hasText(request.resultJson())) {
            emitter.completeWithError(new ApiException(ErrorCode.CHART_RESULT_EMPTY));
            return;
        }
        if (!StringUtils.hasText(apiKey)) {
            emitter.completeWithError(new ApiException(ErrorCode.AI_CONFIG_MISSING));
            return;
        }

        if (request.recordId() != null && !Boolean.TRUE.equals(request.force())) {
            AiAnalysis cached = getCached(request.recordId());
            if (cached != null) {
                try {
                    String content = cached.getContent();
                    int chunkSize = 200;
                    for (int i = 0; i < content.length(); i += chunkSize) {
                        emitter.send(content.substring(i, Math.min(i + chunkSize, content.length())));
                    }
                    emitter.complete();
                } catch (Exception e) {
                    try { emitter.completeWithError(e); } catch (Exception ignored) {}
                }
                return;
            }
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "stream", true,
                    "temperature", 0.5,
                    "top_p", 0.9,
                    "max_tokens", 10000,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(request.resultJson()))
                    )
            );
            String bodyJson = objectMapper.writeValueAsString(requestBody);

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(connectTimeout))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofMillis(readTimeout))
                    .build();

            StringBuilder accumulated = new StringBuilder();
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        String content = node.path("choices").path(0).path("delta").path("content").asText("");
                        if (!content.isEmpty()) {
                            emitter.send(content);
                            accumulated.append(content);
                        }
                    } catch (Exception ignored) {}
                }
            }
            emitter.complete();
            if (request.recordId() != null && accumulated.length() > 0) {
                saveCache(request.recordId(), accumulated.toString());
            }
        } catch (Exception e) {
            log.error("DeepSeek stream failed", e);
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
        }
    }

    private AiAnalysis getCached(Long recordId) {
        return aiAnalysisMapper.selectOne(
                new LambdaQueryWrapper<AiAnalysis>().eq(AiAnalysis::getRecordId, recordId)
        );
    }

    private void saveCache(Long recordId, String content) {
        AiAnalysis existing = getCached(recordId);
        if (existing != null) {
            aiAnalysisMapper.update(null,
                    new LambdaUpdateWrapper<AiAnalysis>()
                            .eq(AiAnalysis::getRecordId, recordId)
                            .set(AiAnalysis::getContent, content)
            );
        } else {
            AiAnalysis entity = new AiAnalysis();
            entity.setRecordId(recordId);
            entity.setContent(content);
            aiAnalysisMapper.insert(entity);
        }
    }

    private String systemPrompt() {
        return "你是一位资深八字命理师，请根据我提供的完整八字排盘（出生年、月、日、时及性别）进行系统、深入、全面的分析。\n" +
                "\n" +
                "要求：\n" +
                "\n" +
                "1. 先完整解析八字各柱（年、月、日、时）及天干地支。\n" +
                "2. 对十神、五行、生克制化、格局、用神忌神等进行详细分析。\n" +
                "3. 输出内容必须覆盖以下维度：\n" +
                "\n" +
                "【基础命局分析】\n" +
                "- 命主出生八字排盘概览\n" +
                "- 日元强弱分析（身强身弱）\n" +
                "- 五行分布及平衡情况\n" +
                "- 十神分布及作用\n" +
                "- 八字格局（正格、偏格、特殊格局等）\n" +
                "- 喜用神与忌神分析\n" +
                "- 命局优势与潜在短板\n" +
                "\n" +
                "【性格与能力分析】\n" +
                "- 核心性格特征\n" +
                "- 思维模式与情绪倾向\n" +
                "- 行事风格与人际交往特点\n" +
                "- 天赋与优势能力\n" +
                "- 容易出现的性格弱点或阻碍\n" +
                "\n" +
                "【事业与职业分析】\n" +
                "- 适合的行业与岗位类型\n" +
                "- 职业发展潜力与特点\n" +
                "- 管理与合作能力\n" +
                "- 创业或就业倾向\n" +
                "- 职场机遇与挑战\n" +
                "\n" +
                "【财运分析】\n" +
                "- 正财、偏财及财富积累能力\n" +
                "- 投资理财倾向\n" +
                "- 财运起伏规律及高峰期\n" +
                "- 财运风险提示\n" +
                "\n" +
                "【感情与婚姻分析】\n" +
                "- 感情观与异性缘\n" +
                "- 婚姻稳定性及关键时间点\n" +
                "- 适合伴侣类型\n" +
                "- 婚姻可能遇到的挑战与解决建议\n" +
                "\n" +
                "【家庭关系】\n" +
                "- 与父母、兄弟姐妹、子女关系特点\n" +
                "- 家庭责任与影响\n" +
                "- 家庭运势对个人命局的作用\n" +
                "\n" +
                "【健康分析】\n" +
                "- 五行对应身体健康隐患\n" +
                "- 容易出现的疾病或身体问题\n" +
                "- 养生及健康改善建议\n" +
                "\n" +
                "【大运分析】\n" +
                "- 每一步大运的开始年份及影响\n" +
                "- 事业、财运、感情、健康变化\n" +
                "- 运势高低及关键转折点\n" +
                "- 每步大运的注意事项及建议\n" +
                "\n" +
                "【流年分析（未来10年）】\n" +
                "- 每年流年天干地支分析\n" +
                "- 对事业、财运、感情、健康的影响\n" +
                "- 风险预警及机会提示\n" +
                "\n" +
                "【综合总结】\n" +
                "- 命局核心特征\n" +
                "- 人生优势与课题\n" +
                "- 关键决策建议\n" +
                "- 最值得重点把握的方向\n" +
                "\n" +
                "输出要求：\n" +
                "1. 分章节、条理清晰，便于阅读。\n" +
                "2. 每个分析结论必须有依据说明。\n" +
                "3. 对重要结论给出可信度（高/中/低）。\n" +
                "4. 尽量详细，内容完整，字数不少于2500字。\n" +
                "5. 如存在不同流派解释，可同时列出并说明理由。"
                + "不要编造 JSON 中没有的信息，不要给医疗、法律、投资等高风险决策建议。";
    }

    private String userPrompt(String resultJson) {
        return "请分析下面这个四柱八字排盘结果 JSON，输出：1. 基础格局 2. 五行强弱 3. 性格倾向 "
                + "4. 事业财运 5. 感情家庭 6. 流年提醒。JSON：\n" + resultJson;
    }
}
