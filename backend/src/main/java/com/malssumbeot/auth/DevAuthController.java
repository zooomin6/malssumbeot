package com.malssumbeot.auth;

import com.malssumbeot.user.AuthProvider;
import com.malssumbeot.user.User;
import com.malssumbeot.user.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발 전용 토큰 발급. {@code dev} 프로파일에서만 활성화되며, 실제 소셜 로그인 없이
 * 고정 테스트 사용자로 자체 JWT를 즉시 내준다. 로그인 플로우(Phase 3)가 완성되기 전
 * 모바일 앱의 {@code /api/chat} 연동을 검증하기 위한 임시 경로 — 로그인 완성 후 제거 대상.
 */
@RestController
@RequestMapping("/api/auth")
@Profile("dev")
public class DevAuthController {

    private static final String DEV_USER_PROVIDER_ID = "dev-test-user";

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public DevAuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/dev-token")
    public LoginResponse devToken() {
        User user = userRepository.findByProviderAndProviderId(AuthProvider.DEV, DEV_USER_PROVIDER_ID)
                .orElseGet(() -> userRepository.save(
                        new User(AuthProvider.DEV, DEV_USER_PROVIDER_ID, null, "개발용 테스트 계정")));
        return LoginResponse.from(user, jwtService.issue(user));
    }
}
