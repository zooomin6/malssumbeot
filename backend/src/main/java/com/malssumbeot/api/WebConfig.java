package com.malssumbeot.api;

import com.malssumbeot.auth.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 계층 배선. JWT 인증 게이트를 요청 길목에 건다.
 *
 * {@code /api/**}는 보호하되, 로그인 자체인 {@code /api/auth/**}는 제외한다 —
 * 출입증을 받으러 오는 곳에서 출입증을 요구할 수는 없기 때문이다(현재 실제 보호 대상은 {@code /api/chat}).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
