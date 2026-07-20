package com.malssumbeot.auth;

import com.malssumbeot.user.AuthProvider;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 소셜 로그인 엔드포인트 (방식 A, D-022). {@code POST /api/auth/{provider}} — provider는 google|kakao.
 * 앱이 받은 제공자 토큰을 바디로 보내면 검증 후 우리 자체 JWT를 돌려준다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/{provider}")
    public LoginResponse login(@PathVariable String provider, @Valid @RequestBody LoginRequest request) {
        return authService.login(parseProvider(provider), request.token());
    }

    private AuthProvider parseProvider(String provider) {
        try {
            return AuthProvider.valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UnsupportedProviderException(provider);
        }
    }
}
