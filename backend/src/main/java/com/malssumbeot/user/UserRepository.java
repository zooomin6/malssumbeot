package com.malssumbeot.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 소셜 로그인 콜백에서 기존 사용자를 찾거나 신규 생성 여부를 판단할 때 쓴다. */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
