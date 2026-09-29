package ru.yandex.practicum.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceAudienceTokenCustomizerTest {

    private final ResourceAudienceTokenCustomizer customizer = new ResourceAudienceTokenCustomizer(
            new ResourceServerProperties(Map.of(
                    "payment-service", Set.of("payments"),
                    "delivery-service", Set.of("delivery"))));

    @Test
    void accessToken_withResourceScope_getsResourceAudience() {
        JwtClaimsSet claims = customize(OAuth2TokenType.ACCESS_TOKEN, Set.of("payments"));

        assertThat(claims.getAudience()).containsExactly("payment-service");
    }

    @Test
    void accessToken_withScopesOfSeveralResources_getsAllAudiences() {
        JwtClaimsSet claims = customize(OAuth2TokenType.ACCESS_TOKEN, Set.of("payments", "delivery"));

        assertThat(claims.getAudience()).containsExactly("delivery-service", "payment-service");
    }

    @Test
    void accessToken_withoutResourceScopes_keepsOriginalAudience() {
        JwtClaimsSet claims = customize(OAuth2TokenType.ACCESS_TOKEN, Set.of("profile"));

        assertThat(claims.getAudience()).containsExactly("market-app");
    }

    @Test
    void refreshToken_isNotChanged() {
        JwtClaimsSet claims = customize(OAuth2TokenType.REFRESH_TOKEN, Set.of("payments"));

        assertThat(claims.getAudience()).containsExactly("market-app");
    }

    private JwtClaimsSet customize(OAuth2TokenType tokenType, Set<String> scopes) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject("market-app").audience(List.of("market-app"));
        JwtEncodingContext context = JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claims)
                .tokenType(tokenType)
                .authorizedScopes(scopes)
                .build();
        customizer.customize(context);
        return claims.build();
    }
}
