package ru.yandex.practicum.mymarket.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.reactive.result.view.CsrfRequestDataValueProcessor;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class SecurityModelAdvice {

    public static final String SECURITY_CONTEXT_ATTRIBUTE = "thymeleafSpringSecurityContext";
    public static final String ADMIN_ATTRIBUTE = "isAdmin";

    @ModelAttribute(CsrfRequestDataValueProcessor.DEFAULT_CSRF_ATTR_NAME)
    public Mono<CsrfToken> csrfToken(ServerWebExchange exchange) {
        Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            return Mono.empty();
        }
        return csrfToken.doOnSuccess(token -> exchange.getAttributes()
                .put(CsrfRequestDataValueProcessor.DEFAULT_CSRF_ATTR_NAME, token));
    }

    @ModelAttribute(SECURITY_CONTEXT_ATTRIBUTE)
    public Mono<SecurityContext> securityContext() {
        return ReactiveSecurityContextHolder.getContext();
    }

    @ModelAttribute(ADMIN_ATTRIBUTE)
    public Mono<Boolean> isAdmin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication() != null
                        && context.getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals))
                .defaultIfEmpty(false);
    }
}
