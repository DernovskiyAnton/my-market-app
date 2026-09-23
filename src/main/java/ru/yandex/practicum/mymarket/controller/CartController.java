package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public String getCart(Model model) {
        return renderCart(model);
    }

    @PostMapping
    public String changeCartItem(@RequestParam long id, @RequestParam CartAction action, Model model) {
        cartService.changeQuantity(id, action);
        return renderCart(model);
    }

    private String renderCart(Model model) {
        CartDto cart = cartService.getCart();
        model.addAttribute("items", cart.items());
        model.addAttribute("total", cart.total());
        return "cart";
    }
}
