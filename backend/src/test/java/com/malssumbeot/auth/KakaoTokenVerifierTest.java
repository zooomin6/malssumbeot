package com.malssumbeot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.malssumbeot.user.AuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoTokenVerifierTest {

    private static final String URI = "https://kapi.kakao.com/v2/user/me";

    @Test
    void 사용자정보_응답에서_신원을_추출한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess(
                        "{\"id\":123456789,\"kakao_account\":{\"email\":\"a@example.com\","
                                + "\"profile\":{\"nickname\":\"민규\"}}}",
                        MediaType.APPLICATION_JSON));
        KakaoTokenVerifier verifier = new KakaoTokenVerifier(builder, URI);

        SocialUser user = verifier.verify("tok");

        assertThat(user.provider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(user.providerId()).isEqualTo("123456789");
        assertThat(user.email()).isEqualTo("a@example.com");
        assertThat(user.nickname()).isEqualTo("민규");
        server.verify();
    }

    @Test
    void 닉네임이_properties에만_있으면_거기서_찾는다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URI))
                .andRespond(withSuccess(
                        "{\"id\":42,\"properties\":{\"nickname\":\"바나바\"}}",
                        MediaType.APPLICATION_JSON));
        KakaoTokenVerifier verifier = new KakaoTokenVerifier(builder, URI);

        SocialUser user = verifier.verify("tok");

        assertThat(user.providerId()).isEqualTo("42");
        assertThat(user.email()).isNull();
        assertThat(user.nickname()).isEqualTo("바나바");
    }

    @Test
    void 유효하지_않은_토큰은_예외로_변환한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URI)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        KakaoTokenVerifier verifier = new KakaoTokenVerifier(builder, URI);

        assertThatThrownBy(() -> verifier.verify("bad-token"))
                .isInstanceOf(InvalidSocialTokenException.class);
    }
}
