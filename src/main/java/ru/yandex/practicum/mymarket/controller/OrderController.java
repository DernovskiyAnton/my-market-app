package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.service.OrderService;

/**
 * Оформление заказа и просмотр заказов.
 */
@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public String buy(RedirectAttributes redirectAttributes) {
        long orderId = orderService.createOrderFromCart();
        redirectAttributes.addAttribute("id", orderId);
        redirectAttributes.addAttribute("newOrder", true);
        return "redirect:/orders/{id}";
    }

    @GetMapping("/orders")
    public String getOrders(Model model) {
        model.addAttribute("orders", orderService.findAll());
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable long id,
                           @RequestParam(defaultValue = "false") boolean newOrder,
                           Model model) {
        model.addAttribute("order", orderService.getOrder(id));
        model.addAttribute("newOrder", newOrder);
        return "order";
    }
}
