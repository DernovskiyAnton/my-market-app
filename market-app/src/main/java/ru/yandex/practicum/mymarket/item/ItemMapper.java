package ru.yandex.practicum.mymarket.item;

public final class ItemMapper {

    private ItemMapper() {
    }

    public static ItemSummary toSummary(Item item) {
        return new ItemSummary(item.getId(), item.getTitle(), item.getDescription(), item.getPrice());
    }

    public static ItemCard toCard(Item item) {
        return new ItemCard(item.getId(), item.getTitle(), item.getDescription(), item.getImgPath(), item.getPrice());
    }

    public static ItemDto toDto(ItemCard card, int count) {
        return new ItemDto(card.id(), card.title(), card.description(), card.imgPath(), card.price(), count);
    }
}
