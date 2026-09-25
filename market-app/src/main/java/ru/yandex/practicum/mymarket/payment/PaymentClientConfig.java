package ru.yandex.practicum.mymarket.payment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;

@Configuration
public class PaymentClientConfig {

    @Bean
    public PaymentsApi paymentsApi(WebClient.Builder webClientBuilder, PaymentProperties properties) {
        WebClient webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient))
                .build()
                .createClient(PaymentsApi.class);
    }
}
