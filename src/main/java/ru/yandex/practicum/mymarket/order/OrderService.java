package ru.yandex.practicum.mymarket.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.common.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public List<OrderDto> findAll() {
        return orderRepository.findAllByOrderByIdDesc().stream().map(OrderMapper::toDto).toList();
    }

    public OrderDto getOrder(long id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderMapper::toDto)
                .orElseThrow(() -> NotFoundException.order(id));
    }

    @Transactional
    public long create(Order order) {
        return orderRepository.save(order).getId();
    }
}
