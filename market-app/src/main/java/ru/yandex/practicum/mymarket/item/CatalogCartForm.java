package ru.yandex.practicum.mymarket.item;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.mymarket.cart.CartAction;

public record CatalogCartForm(@NotNull Long id,
                              String search,
                              SortType sort,
                              @Min(1) Integer pageNumber,
                              @Min(1) @Max(ItemController.MAX_PAGE_SIZE) Integer pageSize,
                              @NotNull CartAction action) {

    public CatalogCartForm {
        search = search == null ? "" : search;
        sort = sort == null ? SortType.NO : sort;
        pageNumber = pageNumber == null ? ItemController.DEFAULT_PAGE_NUMBER : pageNumber;
        pageSize = pageSize == null ? ItemController.DEFAULT_PAGE_SIZE : pageSize;
    }
}
