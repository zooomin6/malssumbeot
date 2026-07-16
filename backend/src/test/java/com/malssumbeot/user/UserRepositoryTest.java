package com.malssumbeot.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 제공자와_제공자ID로_사용자를_찾는다() {
        userRepository.save(new User(AuthProvider.GOOGLE, "google-sub-123", "a@example.com", "민규"));

        var found = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-sub-123");

        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("민규");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void 다른_제공자의_같은_ID는_다른_사용자다() {
        userRepository.save(new User(AuthProvider.GOOGLE, "same-id", null, null));

        assertThat(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "same-id")).isEmpty();
    }
}
