package ru.yandex.practicum.mymarket.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    private Long id;

    private Long userId;

    private LocalDateTime createdAt;

    private long totalSum;

    public Order(Long userId, LocalDateTime createdAt, long totalSum) {
        this.userId = userId;
        this.createdAt = createdAt;
        this.totalSum = totalSum;
    }
}
