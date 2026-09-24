package ru.yandex.practicum.mymarket.dto;

import ru.yandex.practicum.mymarket.item.ItemDto;

import java.util.List;

public record CartDto(List<ItemDto> items, long total) {
}
