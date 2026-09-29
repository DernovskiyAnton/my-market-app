package ru.yandex.practicum.mymarket.order;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.user.MarketUser;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public Mono<Rendering> getOrders(@AuthenticationPrincipal MarketUser user) {
        return orderService.findAll(user.getId())
                .collectList()
                .map(orders -> Rendering.view("orders").modelAttribute("orders", orders).build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrder(@PathVariable long id,
                                    @RequestParam(defaultValue = "false") boolean newOrder,
                                    @AuthenticationPrincipal MarketUser user) {
        return orderService.getOrder(user.getId(), id)
                .map(order -> Rendering.view("order")
                        .modelAttribute("order", order)
                        .modelAttribute("newOrder", newOrder)
                        .build());
    }
}
