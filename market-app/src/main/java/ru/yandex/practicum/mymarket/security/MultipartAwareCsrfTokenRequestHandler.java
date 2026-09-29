package ru.yandex.practicum.mymarket.security;

import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestHandler;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class MultipartAwareCsrfTokenRequestHandler implements ServerCsrfTokenRequestHandler {

    private final XorServerCsrfTokenRequestAttributeHandler delegate = new XorServerCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(ServerWebExchange exchange, Mono<CsrfToken> csrfToken) {
        delegate.handle(exchange, csrfToken);
    }

    @Override
    public Mono<String> resolveCsrfTokenValue(ServerWebExchange exchange, CsrfToken csrfToken) {
        return delegate.resolveCsrfTokenValue(exchange, csrfToken)
                .switchIfEmpty(Mono.defer(() -> tokenFromMultipartData(exchange, csrfToken)
                        .flatMap(token -> delegate.resolveCsrfTokenValue(withTokenHeader(exchange, csrfToken, token),
                                csrfToken))));
    }

    private static Mono<String> tokenFromMultipartData(ServerWebExchange exchange, CsrfToken csrfToken) {
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        if (contentType == null || !MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
            return Mono.empty();
        }
        return exchange.getMultipartData()
                .flatMap(parts -> Mono.justOrEmpty(parts.getFirst(csrfToken.getParameterName())))
                .ofType(FormFieldPart.class)
                .map(FormFieldPart::value);
    }

    private static ServerWebExchange withTokenHeader(ServerWebExchange exchange, CsrfToken csrfToken, String token) {
        return exchange.mutate()
                .request(request -> request.header(csrfToken.getHeaderName(), token))
                .build();
    }
}
