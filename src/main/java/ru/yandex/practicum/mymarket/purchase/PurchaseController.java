package ru.yandex.practicum.mymarket.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/buy")
    public String buy(RedirectAttributes redirectAttributes) {
        long orderId = purchaseService.buy();
        redirectAttributes.addAttribute("id", orderId);
        redirectAttributes.addAttribute("newOrder", true);
        return "redirect:/orders/{id}";
    }
}
