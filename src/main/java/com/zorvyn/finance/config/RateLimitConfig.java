package com.zorvyn.finance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorvyn.finance.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor()).addPathPatterns("/api/**");
    }

    private class RateLimitInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws IOException {

            String clientIp = getClientIp(request);
            long now = System.currentTimeMillis();

            requestCounts.putIfAbsent(clientIp, new CopyOnWriteArrayList<>());
            CopyOnWriteArrayList<Long> timestamps = requestCounts.get(clientIp);

            // remove timestamps older than the window
            timestamps.removeIf(t -> now - t > WINDOW_MS);

            if (timestamps.size() >= MAX_REQUESTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.findAndRegisterModules();
                objectMapper.writeValue(response.getWriter(),
                        ApiResponse.error("Rate limit exceeded. Try again in a minute."));
                return false;
            }

            timestamps.add(now);
            return true;
        }

        private String getClientIp(HttpServletRequest request) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
    }
}
