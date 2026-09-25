package ru.yandex.practicum.mymarket.item;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("items")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    @Id
    private Long id;

    private String title;

    private String description;

    private String imgPath;

    private long price;

    public Item(String title, String description, String imgPath, long price) {
        this.title = title;
        this.description = description;
        this.imgPath = imgPath;
        this.price = price;
    }
}
