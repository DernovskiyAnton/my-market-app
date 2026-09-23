package ru.yandex.practicum.mymarket.dto;

import org.springframework.data.domain.Page;

/**
 * Параметры пагинации для страницы витрины.
 *
 * @param pageNumber номер текущей страницы, начиная с 1
 */
public record Paging(int pageSize, int pageNumber, boolean hasPrevious, boolean hasNext) {

    public static Paging of(Page<?> page) {
        return new Paging(page.getSize(), page.getNumber() + 1, page.hasPrevious(), page.hasNext());
    }
}
