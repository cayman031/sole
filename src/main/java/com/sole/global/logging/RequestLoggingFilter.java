package com.sole.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 단위로 traceId를 생성해 MDC에 주입하고, 처리 시간을 로깅한다.
 * 슬로우 요청(기본 1000ms 초과)은 WARN 레벨로 표시한다.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final long SLOW_THRESHOLD_MS = 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        MDC.put("traceId", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();

            if (elapsed > SLOW_THRESHOLD_MS) {
                log.warn("slow request traceId={} method={} uri={} status={} elapsedMs={}", traceId, method, uri, status, elapsed);
            } else {
                log.info("request traceId={} method={} uri={} status={} elapsedMs={}", traceId, method, uri, status, elapsed);
            }
            MDC.clear();
        }
    }
}
