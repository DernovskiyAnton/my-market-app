package ru.yandex.practicum.mymarket.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.common.NotFoundException;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Flux<OrderDto> findAll() {
        return orderRepository.findAllByOrderByIdDesc()
                .collectList()
                .flatMapMany(this::withItems);
    }

    public Mono<OrderDto> getOrder(long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(() -> NotFoundException.order(id)))
                .flatMap(order -> orderItemRepository.findAllByOrderIdOrderByIdAsc(id)
                        .collectList()
                        .map(items -> OrderMapper.toDto(order, items)));
    }

    @Transactional
    public Mono<Long> create(Order order, List<OrderItem> items) {
        return orderRepository.save(order)
                .flatMap(saved -> {
                    items.forEach(item -> item.setOrderId(saved.getId()));
                    return orderItemRepository.saveAll(items).then(Mono.just(saved.getId()));
                });
    }

    private Flux<OrderDto> withItems(List<Order> orders) {
        if (orders.isEmpty()) {
            return Flux.empty();
        }
        Collection<Long> ids = orders.stream().map(Order::getId).toList();
        return orderItemRepository.findAllByOrderIdInOrderByIdAsc(ids)
                .collectMultimap(OrderItem::getOrderId)
                .flatMapMany(itemsByOrder -> Flux.fromIterable(orders)
                        .map(order -> OrderMapper.toDto(order,
                                List.copyOf(itemsByOrder.getOrDefault(order.getId(), List.of())))));
    }
}
