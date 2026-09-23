package ru.yandex.practicum.mymarket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ImageService;
import ru.yandex.practicum.mymarket.service.ItemImportService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.OrderService;

/**
 * Общая конфигурация тестов веб-слоя. Поднимаются все контроллеры, а сервисы
 * заменяются моками в одном месте, поэтому у всех наследников одинаковый ключ
 * кеша и контекст Spring создаётся один раз.
 */
@WebMvcTest
abstract class ControllerTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected ItemService itemService;

    @MockitoBean
    protected CartService cartService;

    @MockitoBean
    protected OrderService orderService;

    @MockitoBean
    protected ImageService imageService;

    @MockitoBean
    protected ItemImportService itemImportService;
}
