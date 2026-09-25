package ru.yandex.practicum.mymarket.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import ru.yandex.practicum.mymarket.item.Item;

@Table("order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    private Long id;

    private Long orderId;

    private Long itemId;

    private String title;

    private long price;

    private int quantity;

    public OrderItem(Item item, int quantity) {
        this.itemId = item.getId();
        this.title = item.getTitle();
        this.price = item.getPrice();
        this.quantity = quantity;
    }

    public long getSum() {
        return price * quantity;
    }
}
