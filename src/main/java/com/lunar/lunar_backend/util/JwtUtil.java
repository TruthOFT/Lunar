package com.lunar.lunar_backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class JwtUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SECRET = "lunar-jwt-secret-2026";
    private static final long EXPIRE_SECONDS = 7 * 24 * 60 * 60;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private JwtUtil() {
    }

    public static String createToken(Long userId, String account, Integer tokenVersion) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", userId);
            payload.put("account", account);
            payload.put("tokenVersion", tokenVersion);
            payload.put("exp", Instant.now().getEpochSecond() + EXPIRE_SECONDS);

            String headerText = base64Json(header);
            String payloadText = base64Json(payload);
            String signature = sign(headerText + "." + payloadText);
            return headerText + "." + payloadText + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("token create failed", exception);
        }
    }

    public static AuthUser parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new ApiException(ErrorCode.LOGIN_INVALID);
            }
            String signingInput = parts[0] + "." + parts[1];
            if (!sign(signingInput).equals(parts[2])) {
                throw new ApiException(ErrorCode.LOGIN_INVALID);
            }
            byte[] payloadBytes = DECODER.decode(parts[1]);
            Map<String, Object> payload = OBJECT_MAPPER.readValue(payloadBytes, new TypeReference<>() {
            });
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new ApiException(ErrorCode.LOGIN_EXPIRED);
            }
            Long userId = ((Number) payload.get("userId")).longValue();
            String account = String.valueOf(payload.get("account"));
            Integer tokenVersion = ((Number) payload.get("tokenVersion")).intValue();
            return new AuthUser(userId, account, tokenVersion);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.LOGIN_INVALID);
        }
    }

    private static String base64Json(Map<String, Object> value) throws Exception {
        return ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(value));
    }

    private static String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
