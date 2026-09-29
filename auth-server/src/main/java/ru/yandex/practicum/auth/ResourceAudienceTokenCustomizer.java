package ru.yandex.practicum.auth;

import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ResourceAudienceTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final ResourceServerProperties properties;

    public ResourceAudienceTokenCustomizer(ResourceServerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }
        List<String> audiences = properties.resourceServers().entrySet().stream()
                .filter(resource -> !Collections.disjoint(resource.getValue(), context.getAuthorizedScopes()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!audiences.isEmpty()) {
            context.getClaims().audience(audiences);
        }
    }
}
