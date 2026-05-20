package com.veriweb.veriweb_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MS = 60_000L;

    // 엔드포인트별 분당 요청 허용 수
    private static final Map<String, Integer> LIMITS = Map.of(
            "POST:/api/v1/analyze",       5,
            "POST:/api/v1/image/analyze", 3,
            "POST:/api/v1/video/analyze", 3
    );

    private final Map<String, Deque<Long>> timestamps = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String key = request.getMethod() + ":" + request.getRequestURI();
        Integer limit = LIMITS.get(key);
        if (limit == null) return true;

        String clientIp = resolveClientIp(request);
        String bucketKey = clientIp + "|" + key;
        long now = System.currentTimeMillis();

        timestamps.compute(bucketKey, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            deque.addLast(now);
            return deque;
        });

        if (timestamps.get(bucketKey).size() > limit) {
            log.warn("Rate limit 초과: ip={}, endpoint={}", clientIp, key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "message", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
                    "code", "RATE_LIMIT_EXCEEDED"
            )));
            return false;
        }

        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
