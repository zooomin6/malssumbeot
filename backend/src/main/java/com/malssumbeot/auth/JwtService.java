package com.malssumbeot.auth;

import com.malssumbeot.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 로그인 성공 후 발급하는 우리 자체 JWT(엠마오 회원증). 이후 앱은 이 토큰으로 API에 접속한다.
 * subject=사용자 id, provider 클레임을 담는다.
 *
 * secret 미설정 시 임시 키로 부팅한다(AnthropicConfig와 같은 철학 — 키 없이도 부팅 가능).
 * 이 경우 재시작하면 이전에 발급한 토큰은 검증에 실패한다. 운영에선 JWT_SECRET(32바이트 이상) 필수.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${malssumbeot.auth.jwt.secret:}") String secret,
                      @Value("${malssumbeot.auth.jwt.ttl:P30D}") Duration ttl) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET이 설정되지 않았습니다. 임시 키로 부팅합니다 — 재시작 시 기존 토큰은 무효화됩니다.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.ttl = ttl;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("provider", user.getProvider().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** 서명·만료를 검증하고 클레임을 돌려준다. 유효하지 않으면 JwtException을 던진다. */
    public Jws<Claims> parse(String jwt) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt);
    }
}
