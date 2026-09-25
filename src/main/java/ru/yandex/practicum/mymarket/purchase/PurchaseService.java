package ru.yandex.practicum.mymarket.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.cart.CartItem;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.order.Order;
import ru.yandex.practicum.mymarket.order.OrderItem;
import ru.yandex.practicum.mymarket.order.OrderService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final CartService cartService;
    private final OrderService orderService;
    private final Clock clock;

    @Transactional
    public long buy() {
        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }
        Order order = new Order(LocalDateTime.now(clock));
        cartItems.forEach(cartItem -> order.addItem(new OrderItem(cartItem.getItem(), cartItem.getQuantity())));
        long orderId = orderService.create(order);
        cartService.clear();
        return orderId;
    }
}
