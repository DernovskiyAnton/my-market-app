package ru.yandex.practicum.mymarket.cart;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.payment.PaymentService;

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final PaymentService paymentService;

    @GetMapping
    public Mono<Rendering> getCart() {
        return renderCart();
    }

    @PostMapping
    public Mono<Rendering> changeCartItem(@Valid @ModelAttribute CartItemForm form) {
        return cartService.changeQuantity(form.id(), form.action())
                .then(Mono.defer(this::renderCart));
    }

    private Mono<Rendering> renderCart() {
        return cartService.getCart().flatMap(cart -> {
            Rendering.Builder<?> rendering = Rendering.view("cart")
                    .modelAttribute("items", cart.items())
                    .modelAttribute("total", cart.total());
            if (cart.items().isEmpty()) {
                return Mono.just(rendering.build());
            }
            return paymentService.checkAvailability(cart.total())
                    .map(payment -> rendering.modelAttribute("payment", payment).build());
        });
    }
}
