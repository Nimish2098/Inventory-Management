package com.miyuki.Inventory.Management.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final int maxRequestsPerMinute;
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    public RateLimitingFilter(
            @Value("${app.security.rate-limit.auth-per-minute:30}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isAuthEndpoint = "POST".equalsIgnoreCase(method)
                && (path.equals("/auth/login") || path.equals("/auth/register")
                || path.endsWith("/auth/login") || path.endsWith("/auth/register"));

        if (!isAuthEndpoint || maxRequestsPerMinute <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        cleanupExpiredEntries();

        String clientIp = extractClientIp(request);
        long currentWindow = Instant.now().getEpochSecond() / 60;
        String key = clientIp + ":" + currentWindow;

        RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter(currentWindow));
        int count = counter.count.incrementAndGet();

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequestsPerMinute - count)));

        if (count > maxRequestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", "60");

            String uri = escapeJson(request.getRequestURI());
            String body = "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Too many authentication requests. Please try again later.\","
                    + "\"instance\":\"" + uri + "\"}";

            response.getWriter().write(body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > 300_000) { // 5 minutes
            long currentWindow = Instant.now().getEpochSecond() / 60;
            requestCounts.entrySet().removeIf(entry -> entry.getValue().window < currentWindow - 1);
            lastCleanup = now;
        }
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class RequestCounter {
        final long window;
        final AtomicInteger count = new AtomicInteger(0);

        RequestCounter(long window) {
            this.window = window;
        }
    }
}
