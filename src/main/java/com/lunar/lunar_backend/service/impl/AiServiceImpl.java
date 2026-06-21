package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.config.Consts;
import com.coze.openapi.service.service.CozeAPI;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.dto.AiAnalyzeRequest;
import com.lunar.lunar_backend.entity.AiAnalysis;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.AiAnalysisMapper;
import com.lunar.lunar_backend.service.AiService;
import io.reactivex.Flowable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Resource
    private AiAnalysisMapper aiAnalysisMapper;

    @Value("${coze.api-token:}")
    private String apiToken;

    @Value("${coze.bot-id:}")
    private String botId;

    private CozeAPI cozeApi;

    @PostConstruct
    public void initCoze() {
        if (StringUtils.hasText(apiToken)) {
            cozeApi = new CozeAPI.Builder()
                    .baseURL(Consts.COZE_CN_BASE_URL)
                    .auth(new TokenAuth(apiToken))
                    .readTimeout(120000)
                    .build();
            log.info("Coze API initialized: botId={}", botId);
        } else {
            log.warn("Coze API token not configured");
        }
    }

    @Override
    public String analyze(AiAnalyzeRequest request) {
        if (request == null || !StringUtils.hasText(request.resultJson())) {
            throw new ApiException(ErrorCode.CHART_RESULT_EMPTY);
        }
        if (cozeApi == null) {
            throw new ApiException(ErrorCode.AI_CONFIG_MISSING);
        }

        if (request.recordId() != null && !Boolean.TRUE.equals(request.force())) {
            AiAnalysis cached = getCached(request.recordId());
            if (cached != null) {
                return cached.getContent();
            }
        }

        try {
            CreateChatReq req = CreateChatReq.builder()
                    .botID(botId)
                    .userID("backend-user")
                    .messages(Collections.singletonList(
                            Message.buildUserQuestionText(userPrompt(request.resultJson()))
                    ))
                    .build();

            StringBuilder accumulated = new StringBuilder();
            Flowable<ChatEvent> resp = cozeApi.chat().stream(req);
            resp.blockingForEach(event -> {
                if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                    String content = event.getMessage().getContent();
                    if (content != null && !content.isEmpty()) {
                        accumulated.append(content);
                    }
                }
            });

            String result = accumulated.toString();
            if (result.isEmpty()) {
                throw new ApiException(ErrorCode.AI_ANALYZE_FAILED);
            }
            if (request.recordId() != null) {
                saveCache(request.recordId(), result);
            }
            return result;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Coze analyze failed", exception);
            throw new ApiException(ErrorCode.AI_ANALYZE_FAILED, "AI分析失败：" + exception.getMessage());
        }
    }

    @Override
    public void analyzeStream(AiAnalyzeRequest request, SseEmitter emitter) {
        if (request == null || !StringUtils.hasText(request.resultJson())) {
            emitter.completeWithError(new ApiException(ErrorCode.CHART_RESULT_EMPTY));
            return;
        }
        if (cozeApi == null) {
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
            CreateChatReq req = CreateChatReq.builder()
                    .botID(botId)
                    .userID("backend-user")
                    .messages(Collections.singletonList(
                            Message.buildUserQuestionText(userPrompt(request.resultJson()))
                    ))
                    .build();

            StringBuilder accumulated = new StringBuilder();
            Flowable<ChatEvent> resp = cozeApi.chat().stream(req);
            resp.blockingForEach(event -> {
                if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                    String content = event.getMessage().getContent();
                    if (content != null && !content.isEmpty()) {
                        emitter.send(content);
                        accumulated.append(content);
                    }
                }
            });
            emitter.complete();
            if (request.recordId() != null && !accumulated.isEmpty()) {
                saveCache(request.recordId(), accumulated.toString());
            }
        } catch (Exception e) {
            log.error("Coze stream failed", e);
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

    private String userPrompt(String resultJson) {
        return "请分析下面这个四柱八字排盘结果 JSON，输出：1. 基础格局 2. 五行强弱 3. 性格倾向 "
                + "4. 事业财运 5. 感情家庭 6. 流年提醒。JSON：\n" + resultJson;
    }
}
