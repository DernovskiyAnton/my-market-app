package ru.yandex.practicum.mymarket.payment;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class ClientCredentialsExchangeFilter implements ExchangeFilterFunction {

    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;
    private final String clientRegistrationId;
    private final String principalName;

    public ClientCredentialsExchangeFilter(ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
                                           ReactiveOAuth2AuthorizedClientService authorizedClientService,
                                           String clientRegistrationId,
                                           String principalName) {
        this.authorizedClientManager = authorizedClientManager;
        this.authorizedClientService = authorizedClientService;
        this.clientRegistrationId = clientRegistrationId;
        this.principalName = principalName;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistrationId)
                .principal(principalName)
                .build();
        return authorizedClientManager.authorize(authorizeRequest)
                .switchIfEmpty(Mono.error(() -> new ClientAuthorizationRequiredException(clientRegistrationId)))
                .map(client -> ClientRequest.from(request)
                        .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                        .build())
                .flatMap(next::exchange)
                .flatMap(this::forgetTokenIfRejected);
    }

    private Mono<ClientResponse> forgetTokenIfRejected(ClientResponse response) {
        if (response.statusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            return authorizedClientService.removeAuthorizedClient(clientRegistrationId, principalName)
                    .thenReturn(response);
        }
        return Mono.just(response);
    }
}
