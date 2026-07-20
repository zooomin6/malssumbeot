package com.malssumbeot.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 보호된 요청의 JWT(엠마오 회원증)를 검사하는 게이트. 컨트롤러 실행 직전({@code preHandle})에 돌며,
 * {@code Authorization: Bearer <jwt>}를 {@link JwtService#parse}로 검증한다.
 *
 * 검증은 서명 대조(HMAC)만 하는 순수 연산이라 DB·네트워크·상태 저장이 없다(무상태). 통과한 요청은
 * 회원 id를 {@link #USER_ID_ATTRIBUTE} 속성으로 실어 다운스트림(대화 이력 등)이 쓸 수 있게 한다.
 * 배선(경로 매칭)은 {@code api.WebConfig}에서 하며, 로그인 경로 {@code /api/auth/**}는 제외된다.
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    /** 인증된 회원 id(=JWT subject)를 담는 request 속성 키. */
    public static final String USER_ID_ATTRIBUTE = "authUserId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnauthenticatedException("인증 토큰이 없습니다.");
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            String userId = jwtService.parse(token).getPayload().getSubject();
            request.setAttribute(USER_ID_ATTRIBUTE, userId);
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException: 서명 불일치·만료 등 / IllegalArgumentException: 토큰이 비었거나 형식 이상
            throw new UnauthenticatedException("유효하지 않은 인증 토큰입니다.", e);
        }
        return true;
    }
}
