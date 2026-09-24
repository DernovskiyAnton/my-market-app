package ru.yandex.practicum.mymarket.item;

import org.springframework.data.domain.Page;

public record Paging(int pageSize, int pageNumber, boolean hasPrevious, boolean hasNext) {

    public static Paging of(Page<?> page) {
        return new Paging(page.getSize(), page.getNumber() + 1, page.hasPrevious(), page.hasNext());
    }
}
