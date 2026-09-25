package ru.yandex.practicum.mymarket.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/buy")
    public Mono<String> buy() {
        return purchaseService.buy()
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}
