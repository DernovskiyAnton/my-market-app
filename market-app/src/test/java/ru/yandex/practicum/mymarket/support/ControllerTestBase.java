package ru.yandex.practicum.mymarket.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.image.ImageService;
import ru.yandex.practicum.mymarket.item.ItemService;
import ru.yandex.practicum.mymarket.itemimport.ItemImportService;
import ru.yandex.practicum.mymarket.order.OrderService;
import ru.yandex.practicum.mymarket.purchase.PurchaseService;

@WebFluxTest
public abstract class ControllerTestBase {

    @Autowired
    protected WebTestClient webTestClient;

    @MockitoBean
    protected ItemService itemService;

    @MockitoBean
    protected CartService cartService;

    @MockitoBean
    protected OrderService orderService;

    @MockitoBean
    protected PurchaseService purchaseService;

    @MockitoBean
    protected ImageService imageService;

    @MockitoBean
    protected ItemImportService itemImportService;
}
