package ru.yandex.practicum.mymarket.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ImageService;
import ru.yandex.practicum.mymarket.service.ItemImportService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.OrderService;

@WebMvcTest
public abstract class ControllerTestBase {

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
