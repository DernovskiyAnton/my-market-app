package ru.yandex.practicum.mymarket.dto;

/**
 * Товар для отображения на страницах витрины, товара и корзины.
 *
 * @param count количество товара в корзине (0 — товар не в корзине)
 */
public record ItemDto(long id, String title, String description, String imgPath, long price, int count) {

    /**
     * Идентификатор заглушки для пустых ячеек сетки товаров.
     */
    public static final long STUB_ID = -1;

    public static ItemDto stub() {
        return new ItemDto(STUB_ID, "", "", "", 0, 0);
    }
}
