package com.malssumbeot.api;

import com.malssumbeot.auth.JwtAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 인증된 사용자별 요청 횟수를 제한한다. {@link JwtAuthInterceptor} 다음에 실행되도록
 * {@code WebConfig}에 등록 순서를 맞춰, 인증된 회원 id({@link JwtAuthInterceptor#USER_ID_ATTRIBUTE})가
 * 이미 request에 있다고 가정한다.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ChatRateLimiter rateLimiter;

    public RateLimitInterceptor(ChatRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object userId = request.getAttribute(JwtAuthInterceptor.USER_ID_ATTRIBUTE);
        String key = userId != null ? userId.toString() : "anonymous";
        if (!rateLimiter.tryAcquire(key)) {
            throw new RateLimitExceededException("요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return true;
    }
}
