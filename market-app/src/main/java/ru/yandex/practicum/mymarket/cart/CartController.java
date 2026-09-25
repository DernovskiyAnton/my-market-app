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

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

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
        return cartService.getCart()
                .map(cart -> Rendering.view("cart")
                        .modelAttribute("items", cart.items())
                        .modelAttribute("total", cart.total())
                        .build());
    }
}
