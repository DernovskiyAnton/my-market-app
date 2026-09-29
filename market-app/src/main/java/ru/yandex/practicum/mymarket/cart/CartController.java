package ru.yandex.practicum.mymarket.cart;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.payment.PaymentService;
import ru.yandex.practicum.mymarket.user.MarketUser;

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final PaymentService paymentService;

    @GetMapping
    public Mono<Rendering> getCart(@AuthenticationPrincipal MarketUser user) {
        return renderCart(user);
    }

    @PostMapping
    public Mono<Rendering> changeCartItem(@Valid @ModelAttribute CartItemForm form,
                                          @AuthenticationPrincipal MarketUser user) {
        return cartService.changeQuantity(user.getId(), form.id(), form.action())
                .then(Mono.defer(() -> renderCart(user)));
    }

    private Mono<Rendering> renderCart(MarketUser user) {
        return cartService.getCart(user.getId()).flatMap(cart -> {
            Rendering.Builder<?> rendering = Rendering.view("cart")
                    .modelAttribute("items", cart.items())
                    .modelAttribute("total", cart.total());
            if (cart.items().isEmpty()) {
                return Mono.just(rendering.build());
            }
            return paymentService.checkAvailability(user.getUsername(), cart.total())
                    .map(payment -> rendering.modelAttribute("payment", payment).build());
        });
    }
}
