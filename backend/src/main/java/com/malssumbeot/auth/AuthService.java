package com.malssumbeot.auth;

import com.malssumbeot.user.AuthProvider;
import com.malssumbeot.user.User;
import com.malssumbeot.user.UserRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 처리 (방식 A, D-022): 제공자 토큰 검증 → User 조회/생성(upsert) → 자체 JWT 발급.
 * 사용자 식별은 (provider, providerId) 유니크로 한다 — 이메일이 없어도 로그인이 성립한다.
 */
@Service
public class AuthService {

    private final Map<AuthProvider, SocialTokenVerifier> verifiers = new EnumMap<>(AuthProvider.class);
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(List<SocialTokenVerifier> verifierList, UserRepository userRepository,
                       JwtService jwtService) {
        for (SocialTokenVerifier verifier : verifierList) {
            verifiers.put(verifier.provider(), verifier);
        }
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(AuthProvider provider, String token) {
        SocialTokenVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new UnsupportedProviderException(provider.name());
        }
        SocialUser social = verifier.verify(token);
        User user = userRepository.findByProviderAndProviderId(provider, social.providerId())
                .orElseGet(() -> userRepository.save(
                        new User(provider, social.providerId(), social.email(), social.nickname())));
        return LoginResponse.from(user, jwtService.issue(user));
    }
}
