package ru.yandex.practicum.mymarket.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCredentialsExchangeFilterTest {

    private static final String REGISTRATION_ID = "payment-service";

    @Mock
    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private ExchangeFunction next;

    private ClientCredentialsExchangeFilter filter;

    private final ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://payments/api")).build();

    @BeforeEach
    void setUp() {
        filter = new ClientCredentialsExchangeFilter(authorizedClientManager, authorizedClientService,
                REGISTRATION_ID, REGISTRATION_ID);
    }

    @Test
    void filter_addsBearerTokenObtainedForApplicationPrincipal() {
        when(authorizedClientManager.authorize(any())).thenReturn(Mono.just(authorizedClient("token-1")));
        when(next.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));

        StepVerifier.create(filter.filter(request, next))
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        ArgumentCaptor<ClientRequest> sent = ArgumentCaptor.forClass(ClientRequest.class);
        verify(next).exchange(sent.capture());
        assertThat(sent.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-1");
        ArgumentCaptor<OAuth2AuthorizeRequest> authorize = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(authorizedClientManager).authorize(authorize.capture());
        assertThat(authorize.getValue().getClientRegistrationId()).isEqualTo(REGISTRATION_ID);
        assertThat(authorize.getValue().getPrincipal().getName()).isEqualTo(REGISTRATION_ID);
        verify(authorizedClientService, never()).removeAuthorizedClient(any(), any());
    }

    @Test
    void filter_unauthorizedResponse_forgetsCachedToken() {
        when(authorizedClientManager.authorize(any())).thenReturn(Mono.just(authorizedClient("expired")));
        when(next.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()));
        when(authorizedClientService.removeAuthorizedClient(REGISTRATION_ID, REGISTRATION_ID)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(request, next))
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verifyComplete();

        verify(authorizedClientService).removeAuthorizedClient(REGISTRATION_ID, REGISTRATION_ID);
    }

    @Test
    void filter_noAuthorizedClient_failsWithoutSendingRequest() {
        when(authorizedClientManager.authorize(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(request, next))
                .expectError(ClientAuthorizationRequiredException.class)
                .verify();
        verifyNoInteractions(next);
    }

    private static OAuth2AuthorizedClient authorizedClient(String token) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("market-app")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://auth/oauth2/token")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token,
                Instant.now(), Instant.now().plusSeconds(300));
        return new OAuth2AuthorizedClient(registration, REGISTRATION_ID, accessToken);
    }
}
