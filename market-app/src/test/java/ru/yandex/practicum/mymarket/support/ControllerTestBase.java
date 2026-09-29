package ru.yandex.practicum.mymarket.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.image.ImageService;
import ru.yandex.practicum.mymarket.item.ItemService;
import ru.yandex.practicum.mymarket.itemimport.ItemImportService;
import ru.yandex.practicum.mymarket.order.OrderService;
import ru.yandex.practicum.mymarket.payment.PaymentService;
import ru.yandex.practicum.mymarket.purchase.PurchaseService;
import ru.yandex.practicum.mymarket.security.SecurityConfig;
import ru.yandex.practicum.mymarket.user.MarketUser;
import ru.yandex.practicum.mymarket.user.MarketUserDetailsService;
import ru.yandex.practicum.mymarket.user.Role;
import ru.yandex.practicum.mymarket.user.UserRepository;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest
@Import({SecurityConfig.class, MarketUserDetailsService.class})
public abstract class ControllerTestBase {

    protected static final MarketUser ALICE = new MarketUser(1L, "alice", "password", Role.USER);
    protected static final MarketUser ADMIN = new MarketUser(3L, "admin", "password", Role.ADMIN);

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
    protected PaymentService paymentService;

    @MockitoBean
    protected ImageService imageService;

    @MockitoBean
    protected ItemImportService itemImportService;

    @MockitoBean
    protected UserRepository userRepository;

    protected WebTestClient asAlice() {
        return as(ALICE);
    }

    protected WebTestClient asAdmin() {
        return as(ADMIN);
    }

    protected WebTestClient anonymousWithCsrf() {
        return webTestClient.mutateWith(csrf());
    }

    private WebTestClient as(MarketUser user) {
        return webTestClient
                .mutateWith(mockAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())))
                .mutateWith(csrf());
    }
}
