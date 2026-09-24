package ru.yandex.practicum.mymarket.item;

public final class ItemMapper {

    private ItemMapper() {
    }

    public static ItemDto toDto(Item item, int count) {
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(), item.getImgPath(),
                item.getPrice(), count);
    }
}
