package ru.yandex.practicum.mymarket.cart;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("cart_items")
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    @Id
    private Long id;

    private Long userId;

    private Long itemId;

    private int quantity;

    public CartItem(Long userId, Long itemId, int quantity) {
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
    }
}
