package ru.yandex.practicum.mymarket.support;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaymentServerStub extends Dispatcher {

    public static final String CLIENT_ID = "market-app";
    public static final String CLIENT_SECRET = "market-app-secret";
    public static final String TOKEN_PATH = "/oauth2/token";

    private static final Pattern AMOUNT = Pattern.compile("\"amount\"\\s*:\\s*(\\d+)");
    private static final Pattern ACCOUNT_PATH = Pattern.compile("/api/accounts/([^/]+)/(balance|payments)");

    private final MockWebServer server = new MockWebServer();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private final Map<String, Long> balances = new ConcurrentHashMap<>();
    private final Map<String, MockResponse> overriddenResponses = new ConcurrentHashMap<>();
    private final Set<String> issuedTokens = ConcurrentHashMap.newKeySet();
    private final AtomicInteger tokenCounter = new AtomicInteger();
    private volatile long defaultBalance;
    private volatile boolean available;
    private volatile boolean clientAuthorized;
    private volatile Runnable onPayment;

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

    public String tokenUri() {
        return baseUrl() + TOKEN_PATH;
    }

    public void reset(long balance) {
        defaultBalance = balance;
        balances.clear();
        available = true;
        clientAuthorized = true;
        onPayment = () -> {
        };
        overriddenResponses.clear();
        requests.clear();
    }

    public void setBalance(String account, long balance) {
        balances.put(account, balance);
    }

    public long balance(String account) {
        return balances.getOrDefault(account, defaultBalance);
    }

    public void makeUnavailable() {
        available = false;
    }

    public void rejectClientCredentials() {
        clientAuthorized = false;
    }

    public String lastIssuedToken() {
        return "test-access-token-" + tokenCounter.get();
    }

    public void revokeIssuedTokens() {
        issuedTokens.clear();
    }

    public void respondWith(String path, int status, String jsonBody) {
        overriddenResponses.put(path, json(status, jsonBody));
    }

    public void onPayment(Runnable action) {
        onPayment = action;
    }

    public List<RecordedRequest> apiRequests() {
        return requests.stream().filter(request -> request.getPath().startsWith("/api/")).toList();
    }

    public List<RecordedRequest> tokenRequests() {
        return requests.stream().filter(request -> TOKEN_PATH.equals(request.getPath())).toList();
    }

    public List<String> paymentBodies() {
        return apiRequests().stream()
                .filter(request -> request.getPath().endsWith("/payments"))
                .map(request -> request.getBody().clone().readUtf8())
                .toList();
    }

    @Override
    public synchronized MockResponse dispatch(RecordedRequest request) {
        requests.add(request);
        if (!available) {
            return new MockResponse().setResponseCode(500);
        }
        if (TOKEN_PATH.equals(request.getPath())) {
            MockResponse overriddenToken = overriddenResponses.get(TOKEN_PATH);
            return overriddenToken != null ? overriddenToken : issueToken(request);
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")
                || !issuedTokens.contains(authorization.substring("Bearer ".length()))) {
            return new MockResponse().setResponseCode(401).setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        }
        MockResponse overridden = overriddenResponses.get(request.getPath());
        if (overridden != null) {
            return overridden;
        }
        Matcher account = ACCOUNT_PATH.matcher(request.getPath());
        if (!account.matches()) {
            return new MockResponse().setResponseCode(404);
        }
        String username = account.group(1);
        if ("GET".equals(request.getMethod()) && "balance".equals(account.group(2))) {
            return json(200, "{\"amount\":" + balance(username) + "}");
        }
        if ("POST".equals(request.getMethod()) && "payments".equals(account.group(2))) {
            onPayment.run();
            return pay(username, request.getBody().clone().readUtf8());
        }
        return new MockResponse().setResponseCode(405);
    }

    private MockResponse issueToken(RecordedRequest request) {
        String expected = "Basic " + Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
        String body = request.getBody().clone().readUtf8();
        if (!clientAuthorized || !expected.equals(request.getHeader("Authorization"))) {
            return json(401, "{\"error\":\"invalid_client\"}");
        }
        if (!body.contains("grant_type=client_credentials") || !body.contains("scope=payments")) {
            return json(400, "{\"error\":\"invalid_request\"}");
        }
        String token = "test-access-token-" + tokenCounter.incrementAndGet();
        issuedTokens.add(token);
        return json(200, "{\"access_token\":\"" + token + "\",\"token_type\":\"Bearer\",\"expires_in\":300,"
                + "\"scope\":\"payments\"}");
    }

    private MockResponse pay(String username, String body) {
        Matcher matcher = AMOUNT.matcher(body);
        if (!matcher.find()) {
            return json(400, "{\"code\":\"INVALID_REQUEST\",\"message\":\"Некорректный запрос\"}");
        }
        long amount = Long.parseLong(matcher.group(1));
        long balance = balance(username);
        if (amount > balance) {
            return json(409, "{\"code\":\"INSUFFICIENT_FUNDS\",\"message\":\"Недостаточно средств на балансе\"}");
        }
        balances.put(username, balance - amount);
        return json(200, "{\"amount\":" + amount + ",\"balance\":" + (balance - amount) + "}");
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
