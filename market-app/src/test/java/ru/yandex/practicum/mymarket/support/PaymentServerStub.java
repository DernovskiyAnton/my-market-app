package ru.yandex.practicum.mymarket.support;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaymentServerStub extends Dispatcher {

    private static final Pattern AMOUNT = Pattern.compile("\"amount\"\\s*:\\s*(\\d+)");

    private final MockWebServer server = new MockWebServer();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private volatile long balance;
    private volatile boolean available;

    public PaymentServerStub() {
        server.setDispatcher(this);
        try {
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        reset(0);
    }

    public String baseUrl() {
        return "http://" + server.getHostName() + ":" + server.getPort();
    }

    public void reset(long initialBalance) {
        balance = initialBalance;
        available = true;
        requests.clear();
    }

    public void makeUnavailable() {
        available = false;
    }

    public long balance() {
        return balance;
    }

    public List<RecordedRequest> requests() {
        return List.copyOf(requests);
    }

    public List<String> paymentBodies() {
        return requests.stream()
                .filter(request -> "/api/payments".equals(request.getPath()))
                .map(request -> request.getBody().clone().readUtf8())
                .toList();
    }

    @Override
    public synchronized MockResponse dispatch(RecordedRequest request) {
        requests.add(request);
        if (!available) {
            return new MockResponse().setResponseCode(500);
        }
        if ("GET".equals(request.getMethod()) && "/api/balance".equals(request.getPath())) {
            return json(200, "{\"amount\":" + balance + "}");
        }
        if ("POST".equals(request.getMethod()) && "/api/payments".equals(request.getPath())) {
            Matcher matcher = AMOUNT.matcher(request.getBody().clone().readUtf8());
            if (!matcher.find()) {
                return json(400, "{\"code\":\"INVALID_REQUEST\",\"message\":\"Некорректный запрос\"}");
            }
            long amount = Long.parseLong(matcher.group(1));
            if (amount > balance) {
                return json(409, "{\"code\":\"INSUFFICIENT_FUNDS\",\"message\":\"Недостаточно средств на балансе\"}");
            }
            balance -= amount;
            return json(200, "{\"amount\":" + amount + ",\"balance\":" + balance + "}");
        }
        return new MockResponse().setResponseCode(404);
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
