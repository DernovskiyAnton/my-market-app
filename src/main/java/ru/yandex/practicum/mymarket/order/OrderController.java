package ru.yandex.practicum.mymarket.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
