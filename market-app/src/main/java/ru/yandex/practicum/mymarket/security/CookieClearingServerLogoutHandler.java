package ru.yandex.practicum.mymarket.security;

import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import reactor.core.publisher.Mono;

public class CookieClearingServerLogoutHandler implements ServerLogoutHandler {

    @Override
    public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
        ServerHttpResponse response = exchange.getExchange().getResponse();
        exchange.getExchange().getRequest().getCookies().keySet().forEach(name -> response.addCookie(
                ResponseCookie.from(name, "").path("/").maxAge(0).httpOnly(true).build()));
        return Mono.empty();
    }
}
