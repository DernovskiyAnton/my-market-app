package ru.yandex.practicum.mymarket.payment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;

@Configuration
public class PaymentClientConfig {

    @Bean
    public ReactiveOAuth2AuthorizedClientManager paymentAuthorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    @Bean
    public PaymentsApi paymentsApi(WebClient.Builder webClientBuilder,
                                   PaymentProperties properties,
                                   ReactiveOAuth2AuthorizedClientManager paymentAuthorizedClientManager,
                                   ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        WebClient webClient = webClientBuilder
                .baseUrl(properties.baseUrl())
                .filter(new ClientCredentialsExchangeFilter(paymentAuthorizedClientManager, authorizedClientService,
                        properties.clientRegistrationId(), properties.clientRegistrationId()))
                .build();
        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient))
                .build()
                .createClient(PaymentsApi.class);
    }
}
