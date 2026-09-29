package ru.yandex.practicum.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthServerIntegrationTest {

    private static final String CLIENT_ID = "market-app";
    private static final String CLIENT_SECRET = "market-app-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void clientCredentials_issuesSignedAccessTokenForPaymentService() throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .param("grant_type", "client_credentials")
                        .param("scope", "payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.scope").value("payments"))
                .andExpect(jsonPath("$.expires_in").isNumber())
                .andReturn();

        Jwt jwt = jwtDecoder.decode(tokenValue(result));
        assertThat(jwt.getSubject()).isEqualTo(CLIENT_ID);
        assertThat(jwt.getAudience()).containsExactly("payment-service");
        assertThat(jwt.getClaimAsStringList("scope")).containsExactly("payments");
        assertThat(jwt.getIssuer()).hasToString("http://localhost:9000");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void clientCredentials_withoutScope_issuesTokenWithoutPaymentServiceAudience() throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isOk())
                .andReturn();

        Jwt jwt = jwtDecoder.decode(tokenValue(result));
        assertThat(jwt.getAudience()).doesNotContain("payment-service");
        assertThat(jwt.hasClaim("scope")).isFalse();
    }

    @Test
    void wrongClientSecret_isRejected() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(CLIENT_ID, "wrong-secret"))
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void unknownClient_isRejected() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic("unknown", CLIENT_SECRET))
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void notRegisteredScope_isRejected() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .param("grant_type", "client_credentials")
                        .param("scope", "admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_scope"));
    }

    @Test
    void notAllowedGrantType_isRejected() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .param("grant_type", "authorization_code")
                        .param("code", "any"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jwkSetAndMetadata_arePublished() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys", hasSize(1)))
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:9000"))
                .andExpect(jsonPath("$.token_endpoint").value("http://localhost:9000/oauth2/token"))
                .andExpect(jsonPath("$.jwks_uri").value("http://localhost:9000/oauth2/jwks"));
    }

    private static String tokenValue(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
