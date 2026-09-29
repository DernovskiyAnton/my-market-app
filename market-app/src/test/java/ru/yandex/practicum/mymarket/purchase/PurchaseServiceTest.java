package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartLine;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.item.ItemCard;
import ru.yandex.practicum.mymarket.order.Order;
import ru.yandex.practicum.mymarket.order.OrderItem;
import ru.yandex.practicum.mymarket.order.OrderService;
import ru.yandex.practicum.mymarket.payment.PaymentRejectedException;
import ru.yandex.practicum.mymarket.payment.PaymentService;
import ru.yandex.practicum.mymarket.payment.PaymentUnavailableException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private static final long USER_ID = 1L;

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private TransactionalOperator transactionalOperator;

    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(cartService, orderService, paymentService, transactionalOperator,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buy_paysFirstThenSavesOrderAndClearsCartInTransaction() {
        givenCart();
        givenTransactionalOperatorPassesThrough();
        when(paymentService.pay("alice", 230)).thenReturn(Mono.just(770L));
        when(orderService.create(any(Order.class), anyList())).thenReturn(Mono.just(10L));
        when(cartService.clear(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(purchaseService.buy(USER_ID, "alice"))
                .expectNext(10L)
                .verifyComplete();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        InOrder inOrder = inOrder(paymentService, transactionalOperator, orderService, cartService);
        inOrder.verify(paymentService).pay("alice", 230);
        inOrder.verify(orderService).create(orderCaptor.capture(), itemsCaptor.capture());
        inOrder.verify(transactionalOperator).transactional(any(Mono.class));
        inOrder.verify(cartService).clear(USER_ID);
        assertThat(orderCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(orderCaptor.getValue().getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(orderCaptor.getValue().getTotalSum()).isEqualTo(230);
        assertThat(itemsCaptor.getValue())
                .extracting(OrderItem::getItemId, OrderItem::getTitle, OrderItem::getPrice, OrderItem::getQuantity)
                .containsExactly(tuple(1L, "Товар 1", 100L, 2), tuple(2L, "Товар 2", 30L, 1));
    }

    @Test
    void buy_paymentRejected_doesNotTouchOrdersOrCart() {
        givenCart();
        when(paymentService.pay("alice", 230)).thenReturn(Mono.error(new PaymentRejectedException("Недостаточно средств")));

        StepVerifier.create(purchaseService.buy(USER_ID, "alice"))
                .expectError(PaymentRejectedException.class)
                .verify();
        verify(orderService, never()).create(any(), anyList());
        verify(cartService, never()).clear(anyLong());
        verifyNoInteractions(transactionalOperator);
    }

    @Test
    void buy_paymentServiceUnavailable_doesNotTouchOrdersOrCart() {
        givenCart();
        when(paymentService.pay("alice", 230)).thenReturn(Mono.error(new PaymentUnavailableException(new RuntimeException())));

        StepVerifier.create(purchaseService.buy(USER_ID, "alice"))
                .expectError(PaymentUnavailableException.class)
                .verify();
        verify(orderService, never()).create(any(), anyList());
        verify(cartService, never()).clear(anyLong());
        verifyNoInteractions(transactionalOperator);
    }

    @Test
    void buy_orderSavingFailsAfterPayment_returnsErrorAndKeepsCart() {
        givenCart();
        givenTransactionalOperatorPassesThrough();
        when(paymentService.pay("alice", 230)).thenReturn(Mono.just(770L));
        when(orderService.create(any(Order.class), anyList())).thenReturn(Mono.error(new IllegalStateException("db")));

        StepVerifier.create(purchaseService.buy(USER_ID, "alice"))
                .expectErrorMessage("db")
                .verify();
        verify(cartService, never()).clear(anyLong());
    }

    @Test
    void buy_emptyCart_returnsErrorWithoutPayment() {
        when(cartService.getCartLines(USER_ID)).thenReturn(Flux.empty());

        StepVerifier.create(purchaseService.buy(USER_ID, "alice"))
                .expectError(EmptyCartException.class)
                .verify();
        verify(orderService, never()).create(any(), anyList());
        verify(paymentService, never()).pay(anyString(), anyLong());
        verify(cartService, never()).clear(anyLong());
    }

    @SuppressWarnings("unchecked")
    private void givenTransactionalOperatorPassesThrough() {
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenCart() {
        when(cartService.getCartLines(USER_ID)).thenReturn(Flux.just(
                new CartLine(item(1L, 100), 2),
                new CartLine(item(2L, 30), 1)));
    }

    private static ItemCard item(long id, long price) {
        return new ItemCard(id, "Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
    }
}
