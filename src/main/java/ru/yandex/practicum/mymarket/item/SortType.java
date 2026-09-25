package ru.yandex.practicum.mymarket.item;

import org.springframework.data.domain.Sort;

public enum SortType {
    NO(Sort.unsorted()),
    ALPHA(Sort.by("title").ascending()),
    PRICE(Sort.by("price").ascending());

    private final Sort sort;

    SortType(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
