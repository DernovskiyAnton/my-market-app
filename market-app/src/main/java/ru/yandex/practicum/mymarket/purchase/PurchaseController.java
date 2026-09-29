package ru.yandex.practicum.mymarket.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.user.MarketUser;

@Controller
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/buy")
    public Mono<String> buy(@AuthenticationPrincipal MarketUser user) {
        return purchaseService.buy(user.getId(), user.getUsername())
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}
