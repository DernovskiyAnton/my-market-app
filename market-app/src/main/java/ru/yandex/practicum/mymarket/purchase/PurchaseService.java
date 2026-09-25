package ru.yandex.practicum.mymarket.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartLine;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.order.Order;
import ru.yandex.practicum.mymarket.order.OrderItem;
import ru.yandex.practicum.mymarket.order.OrderService;
import ru.yandex.practicum.mymarket.payment.PaymentService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final Clock clock;

    @Transactional
    public Mono<Long> buy() {
        return cartService.getCartLines()
                .collectList()
                .flatMap(this::placeOrder)
                .flatMap(orderId -> cartService.clear().thenReturn(orderId));
    }

    private Mono<Long> placeOrder(List<CartLine> lines) {
        if (lines.isEmpty()) {
            return Mono.error(new EmptyCartException());
        }
        List<OrderItem> items = lines.stream()
                .map(line -> new OrderItem(line.item(), line.quantity()))
                .toList();
        long totalSum = items.stream().mapToLong(OrderItem::getSum).sum();
        return orderService.create(new Order(LocalDateTime.now(clock), totalSum), items)
                .flatMap(orderId -> paymentService.pay(totalSum).thenReturn(orderId));
    }
}
