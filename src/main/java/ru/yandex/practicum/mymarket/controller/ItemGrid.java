package ru.yandex.practicum.mymarket.controller;

import ru.yandex.practicum.mymarket.dto.ItemDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Раскладывает товары по строкам сетки витрины. Неполная последняя строка
 * дополняется заглушками, чтобы шаблон вывел пустые ячейки.
 */
final class ItemGrid {

    static final int ROW_SIZE = 3;

    private ItemGrid() {
    }

    static List<List<ItemDto>> toRows(List<ItemDto> items) {
        List<List<ItemDto>> rows = new ArrayList<>();
        for (int from = 0; from < items.size(); from += ROW_SIZE) {
            List<ItemDto> row = new ArrayList<>(items.subList(from, Math.min(from + ROW_SIZE, items.size())));
            while (row.size() < ROW_SIZE) {
                row.add(ItemDto.stub());
            }
            rows.add(row);
        }
        return rows;
    }
}
