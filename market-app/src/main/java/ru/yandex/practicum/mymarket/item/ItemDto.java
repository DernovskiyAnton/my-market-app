package ru.yandex.practicum.mymarket.item;

public record ItemDto(long id, String title, String description, String imgPath, long price, int count) {

    public static final long STUB_ID = -1;

    public static ItemDto stub() {
        return new ItemDto(STUB_ID, "", "", "", 0, 0);
    }
}
