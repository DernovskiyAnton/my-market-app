package ru.yandex.practicum.mymarket.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final TransactionalOperator transactionalOperator;
    private final Clock clock;

    public Mono<Long> buy() {
        return cartService.getCartLines()
                .collectList()
                .flatMap(this::payAndPlaceOrder);
    }

    private Mono<Long> payAndPlaceOrder(List<CartLine> lines) {
        if (lines.isEmpty()) {
            return Mono.error(new EmptyCartException());
        }
        List<OrderItem> items = lines.stream()
                .map(line -> new OrderItem(line.item(), line.quantity()))
                .toList();
        long totalSum = items.stream().mapToLong(OrderItem::getSum).sum();
        return paymentService.pay(totalSum)
                .then(Mono.defer(() -> saveOrderAndClearCart(new Order(LocalDateTime.now(clock), totalSum), items)));
    }

    private Mono<Long> saveOrderAndClearCart(Order order, List<OrderItem> items) {
        return orderService.create(order, items)
                .flatMap(orderId -> cartService.clear().thenReturn(orderId))
                .as(transactionalOperator::transactional)
                .doOnError(error -> log.error("Order for {} руб. was paid but could not be saved", order.getTotalSum(),
                        error));
    }
}
