package ru.yandex.practicum.mymarket.item;

import java.util.Comparator;

public enum SortType {
    NO(Comparator.comparingLong(ItemSummary::id)),
    ALPHA(Comparator.comparing(ItemSummary::title, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(ItemSummary::id)),
    PRICE(Comparator.comparingLong(ItemSummary::price).thenComparingLong(ItemSummary::id));

    private final Comparator<ItemSummary> comparator;

    SortType(Comparator<ItemSummary> comparator) {
        this.comparator = comparator;
    }

    public Comparator<ItemSummary> comparator() {
        return comparator;
    }
}
