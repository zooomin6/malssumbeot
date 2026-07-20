package com.malssumbeot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.malssumbeot.user.AuthProvider;
import com.malssumbeot.user.User;
import com.malssumbeot.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtService jwtService = mock(JwtService.class);

    private AuthService serviceWith(SocialTokenVerifier... verifiers) {
        return new AuthService(List.of(verifiers), userRepository, jwtService);
    }

    private SocialTokenVerifier googleVerifierReturning(SocialUser social) {
        SocialTokenVerifier verifier = mock(SocialTokenVerifier.class);
        when(verifier.provider()).thenReturn(AuthProvider.GOOGLE);
        when(verifier.verify(any())).thenReturn(social);
        return verifier;
    }

    @Test
    void 신규_사용자는_생성하고_JWT를_발급한다() {
        SocialUser social = new SocialUser(AuthProvider.GOOGLE, "sub-1", "a@example.com", "민규");
        AuthService service = serviceWith(googleVerifierReturning(social));
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.issue(any(User.class))).thenReturn("jwt-new");

        LoginResponse response = service.login(AuthProvider.GOOGLE, "provider-token");

        assertThat(response.accessToken()).isEqualTo("jwt-new");
        assertThat(response.provider()).isEqualTo("GOOGLE");
        assertThat(response.nickname()).isEqualTo("민규");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 기존_사용자는_생성하지_않고_재사용한다() {
        SocialUser social = new SocialUser(AuthProvider.GOOGLE, "sub-1", "new@example.com", "새닉");
        AuthService service = serviceWith(googleVerifierReturning(social));
        User existing = new User(AuthProvider.GOOGLE, "sub-1", "old@example.com", "기존닉");
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.of(existing));
        when(jwtService.issue(existing)).thenReturn("jwt-existing");

        LoginResponse response = service.login(AuthProvider.GOOGLE, "provider-token");

        assertThat(response.accessToken()).isEqualTo("jwt-existing");
        assertThat(response.nickname()).isEqualTo("기존닉");
        verify(userRepository, never()).save(any());
    }

    @Test
    void 검증기가_없는_제공자는_거부한다() {
        AuthService service = serviceWith(); // 등록된 검증기 없음

        assertThatThrownBy(() -> service.login(AuthProvider.APPLE, "token"))
                .isInstanceOf(UnsupportedProviderException.class);
    }
}
