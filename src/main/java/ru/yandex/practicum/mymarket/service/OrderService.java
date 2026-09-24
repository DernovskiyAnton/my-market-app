package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final Clock clock;

    public List<OrderDto> findAll() {
        return orderRepository.findAllByOrderByIdDesc().stream().map(OrderMapper::toDto).toList();
    }

    public OrderDto getOrder(long id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderMapper::toDto)
                .orElseThrow(() -> NotFoundException.order(id));
    }

    @Transactional
    public long createOrderFromCart() {
        List<CartItem> cartItems = cartItemRepository.findAllByOrderByIdAsc();
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }
        Order order = new Order(LocalDateTime.now(clock));
        cartItems.forEach(cartItem -> order.addItem(new OrderItem(cartItem.getItem(), cartItem.getQuantity())));
        Order saved = orderRepository.save(order);
        cartService.clear();
        return saved.getId();
    }
}
