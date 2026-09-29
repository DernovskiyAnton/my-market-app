# Витрина интернет-магазина, сервис платежей и сервер авторизации (my-market)

Мультипроект Maven из трёх приложений на Spring Boot:

- **market-app** — веб-приложение «Витрина интернет-магазина» (Spring WebFlux + Thymeleaf, Spring Data R2DBC,
  Spring Security). Покупатели входят по логину и паролю. У каждого из них своя корзина, свои заказы
  и свой счёт в сервисе платежей. Анонимный пользователь видит только витрину и карточки товаров.
  Товары кешируются в Redis.
- **payment-service** — RESTful-сервис платежей на Spring WebFlux, OAuth2 resource server: отдаёт баланс
  счёта пользователя и списывает с него сумму заказа. Принимает запросы только с токеном доступа сервера
  авторизации.
- **auth-server** — сервер авторизации OAuth2 на Spring Authorization Server. Выдаёт витрине токены
  по Client Credentials Flow.

Интеграция витрины и сервиса платежей описана OpenAPI-спецификацией
[`openapi/payment-api.yaml`](openapi/payment-api.yaml). По ней при сборке генерируются HTTP-клиент для витрины
и REST-контроллер для сервиса платежей.

## Стек

| Компонент | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.5 |
| Веб-слой | Spring WebFlux, встроенный Netty; Thymeleaf в витрине. Сервер авторизации — Spring MVC и Tomcat: Spring Authorization Server работает только на сервлетном стеке |
| Безопасность | Spring Security: вход по форме, BCrypt, CSRF (витрина); OAuth2 Client Credentials (витрина → сервис платежей); OAuth2 Resource Server с JWT (сервис платежей); Spring Authorization Server |
| Доступ к данным | Spring Data R2DBC, H2 в памяти (`r2dbc-h2`) |
| Кеш товаров | Redis, Spring Data Redis Reactive |
| Интеграция | OpenAPI 3, OpenAPI Generator 7: клиент `spring-http-interface` поверх `WebClient`, сервер — реактивный контроллер с делегатом |
| Тесты | JUnit 5, Mockito, Reactor Test, Spring Boot Test (`@SpringBootTest`, `@WebFluxTest`, `@DataR2dbcTest`), Spring Security Test, `WebTestClient`, MockMvc, Testcontainers (Redis), MockWebServer, Nimbus JOSE (подпись тестовых JWT) |
| Сборка | Maven (Maven Wrapper), мультимодульный проект, Executable JAR |
| Развёртывание | Docker, Docker Compose |

## Структура мультипроекта

```
.
├── pom.xml                      корневой pom: модули, версии плагинов, общая конфигурация
├── openapi/payment-api.yaml     OpenAPI-спецификация сервиса платежей (с OAuth2-схемой безопасности)
├── docker-compose.yml           Redis + auth-server + payment-service + market-app
├── market-app/                  витрина интернет-магазина (порт 8080)
├── payment-service/             сервис платежей (порт 8081)
└── auth-server/                 сервер авторизации OAuth2 (порт 9000)
```

## Авторизация

```
  браузер ──логин/пароль──▶ market-app ──client_credentials (market-app / секрет)──▶ auth-server
                               │                                     ◀── JWT: aud=payment-service, scope=payments
                               └──Authorization: Bearer <JWT>──▶ payment-service ──JWKS──▶ auth-server
```

### Пользователи витрины

Пользователи хранятся в таблице `users`: логин, BCrypt-хеш пароля и роль. Они заранее загружены
скриптом `data.sql`:

| Логин | Пароль | Роль | Счёт в сервисе платежей |
|---|---|---|---|
| `alice` | `alice123` | `USER` | 50 000 руб. |
| `bob` | `bob123` | `USER` | 1 000 руб. (чтобы проверить нехватку средств) |
| `admin` | `admin123` | `ADMIN` | 50 000 руб. |

Права доступа (`SecurityConfig`):

| Ресурс | Анонимный | `USER` | `ADMIN` |
|---|---|---|---|
| `GET /`, `/items`, `/items/{id}`, `/images/**`, `/login` | да | да | да |
| корзина, изменение количества, `POST /buy`, заказы | нет, перенаправление на `/login?required` | да | да |
| загрузка товаров `/admin/**` | нет, перенаправление на `/login?required` | нет, страница `403` | да |

- **На уровне HTML** анонимный пользователь не видит кнопок корзины и ссылок на корзину и заказы, вместо них
  выводится «Войдите, чтобы купить» и кнопка «Войти». Ссылку на загрузку товаров видит только администратор.
  Шапка страниц вынесена во фрагмент `fragments/header.html`.
- **На уровне эндпоинтов** Spring Security проверяет права до вызова контроллера. Анонимный пользователь
  перенаправляется на страницу входа с пояснением, пользователь без роли `ADMIN` получает страницу
  «Недостаточно прав» со статусом `403`.
- **Изоляция данных.** Контроллеры получают текущего пользователя через `@AuthenticationPrincipal MarketUser`,
  сервисы работают с корзиной и заказами только по его `id`. Чужой заказ не отличить от несуществующего:
  ответ `404`. Покупка идёт только из своей корзины со своего счёта (`/api/accounts/{логин}/…`).
- **Вход** — собственная страница `/login`, пароли проверяются через `BCryptPasswordEncoder`.
- **Выход** (`POST /logout`) удаляет контекст безопасности и сессию (`WebSession`), удаляет все куки
  запроса, включая `SESSION`, и отправляет заголовок `Clear-Site-Data` (браузеры принимают его только
  по HTTPS). После выхода старая сессия недействительна.
- **CSRF** включён для всех изменяющих запросов. Токен подставляется в формы Thymeleaf автоматически
  (`th:action`), а для формы загрузки файлов принимается из multipart-данных (`MultipartAwareCsrfTokenRequestHandler`).
- **Контекст безопасности для шаблонов.** В WebFlux диалект Spring Security для Thymeleaf поддерживает только
  выражения вида `isAuthenticated()` и `isAnonymous()`. Контекст безопасности и признак администратора
  передаются в шаблоны через `SecurityModelAdvice`.

### Сервер авторизации (auth-server)

Spring Authorization Server настраивается свойствами Spring Boot (`spring.security.oauth2.authorizationserver.*`):

- **Издатель токенов:** `http://localhost:9000`, в Docker Compose — `http://auth-server:9000`.
- **Клиент `market-app`:** секрет хранится BCrypt-хешем, аутентификация `client_secret_basic`, grant type
  `client_credentials`, scope `payments`, время жизни токена 5 минут.
- **Сервис платежей как ресурс.** Он описан свойством `auth.resource-servers.payment-service=payments`.
  `ResourceAudienceTokenCustomizer` добавляет в токен аудиторию `payment-service`, если клиенту выдан
  scope `payments`.
- **Эндпоинты:** `POST /oauth2/token`, `GET /oauth2/jwks`, `GET /.well-known/oauth-authorization-server`.

Получить токен вручную:

```bash
curl -s -u market-app:market-app-secret -d grant_type=client_credentials -d scope=payments localhost:9000/oauth2/token
```

### Витрина как OAuth2-клиент

Регистрация клиента `payment-service` — это стандартные свойства `spring.security.oauth2.client.*`. Адрес
сервера авторизации и учётные данные клиента задаются через `market.auth.base-url`, `market.auth.client-id`
и `market.auth.client-secret`.

`PaymentClientConfig` добавляет в `WebClient` сгенерированного клиента фильтр `ClientCredentialsExchangeFilter`.
Фильтр работает так:

- получает токен через `ReactiveOAuth2AuthorizedClientManager` (Client Credentials) от имени самого приложения,
  а не пользователя;
- кеширует токен до истечения срока и добавляет его в заголовок `Authorization: Bearer …`;
- если сервис платежей отвечает `401`, забывает токен, и следующий запрос получает новый.

`PaymentService` разбирает ошибки:

| Ситуация | Что видит пользователь | Статус |
|---|---|---|
| сервер авторизации отказал клиенту (`invalid_client`, `invalid_scope` и т.п.), сервис платежей ответил `401`/`403`/`400`/`404` | «Сервис платежей отклонил запрос…», для отказа в токене — «витрина не авторизована…» | `502` |
| `409` от сервиса платежей | «Оплата не прошла: недостаточно средств…» | `409` |
| сервис платежей или сервер авторизации не отвечает, `5xx`, таймаут | «Сервис платежей недоступен…» | `503` |

На странице корзины в этих случаях вместо кнопки «Купить» показывается сообщение.

### Сервис платежей как resource server

- **Проверка токена.** `SecurityConfig` включает `oauth2ResourceServer().jwt()`. Подпись JWT проверяется
  по JWKS сервера авторизации (`jwk-set-uri`), также проверяются издатель (`issuer-uri`), аудитория
  `payment-service` и срок действия.
- **Доступ к `/api/**`** только с правом `SCOPE_payments`. Без токена или с невалидным токеном — `401`,
  с токеном без scope `payments` — `403`.
- **`/actuator/health`** открыт и используется в healthcheck Docker Compose.

## Сервис платежей (payment-service)

| Метод | Путь | Тело запроса | Ответ |
|---|---|---|---|
| GET | `/api/accounts/{username}/balance` | — | `200 {"amount": 50000}` |
| POST | `/api/accounts/{username}/payments` | `{"amount": 5600}` | `200 {"amount": 5600, "balance": 44400}` |
| | | | `409 INSUFFICIENT_FUNDS` — недостаточно средств, платёж не проведён |
| | | | `400 INVALID_REQUEST` — некорректные сумма или логин |
| | | | `401` — нет токена или он недействителен, `403` — нет scope `payments` |

- **Счета** хранятся в памяти, у каждого пользователя свой. Начальный баланс задаётся свойством
  `payment.account.initial-balance` (50 000), для отдельных пользователей —
  `payment.account.balances.<логин>` (у `bob` 1 000).
- **Списание атомарное** (`AtomicLong.compareAndSet`).
- **Сгенерированный код.** `PaymentsApiController`, `PaymentsApi`, `PaymentsApiDelegate` и модели генерируются
  по спецификации, логика подключается реализацией делегата `PaymentsApiDelegateImpl`.

## Витрина (market-app)

### Возможности

- **Витрина** (`/`, `/items`): поиск по названию и описанию, сортировка по алфавиту и цене, пагинация
  по 2, 5, 10, 20, 50 и 100 товаров. Изменение количества в корзине доступно только после входа.
- **Карточка товара** (`/items/{id}`).
- **Корзина** (`/cart/items`) текущего пользователя: баланс его счёта, кнопка «Купить», если средств хватает,
  иначе сообщение.
- **Покупка** (`POST /buy`): сначала платёж со счёта пользователя, затем в короткой транзакции БД
  записывается заказ и очищается корзина.
- **Заказы** (`/orders`, `/orders/{id}`) текущего пользователя.
- **Загрузка товаров** (`/admin/items`) из CSV вместе с изображениями, только для администратора.

### Эндпоинты

| Метод | Путь | Доступ | Результат |
|---|---|---|---|
| GET | `/`, `/items?search=&sort=NO\|ALPHA\|PRICE&pageNumber=1&pageSize=5` | все | шаблон `items` |
| POST | `/items?id=&action=PLUS\|MINUS&…` | пользователь | `redirect:/items?...` |
| GET | `/items/{id}` | все | шаблон `item` |
| POST | `/items/{id}?action=PLUS\|MINUS` | пользователь | шаблон `item` |
| GET | `/cart/items` | пользователь | шаблон `cart` |
| POST | `/cart/items?id=&action=PLUS\|MINUS\|DELETE` | пользователь | шаблон `cart` |
| POST | `/buy` | пользователь | `redirect:/orders/{id}?newOrder=true` |
| GET | `/orders`, `/orders/{id}` | пользователь | шаблоны `orders`, `order` |
| GET | `/login` | все | шаблон `login` |
| POST | `/login`, `/logout` | все / пользователь | обработка Spring Security |
| GET | `/access-denied` | все | шаблон `error`, статус `403` |
| GET | `/admin/items?imported=N` | администратор | шаблон `import` |
| POST | `/admin/items/import` | администратор | `redirect:/admin/items?imported=N` |
| GET | `/images/{fileName}` | все | файл изображения |

### Структура

```
market-app/src/main/java/ru/yandex/practicum/mymarket
├── user/        пользователи: User, Role, UserRepository, MarketUser (UserDetails с id),
│                MarketUserDetailsService (ReactiveUserDetailsService)
├── security/    SecurityConfig, LoginController, SecurityModelAdvice,
│                MultipartAwareCsrfTokenRequestHandler, CookieClearingServerLogoutHandler
├── item/        товары и кеш в Redis: Item, ItemRepository, ItemService, ItemController, ItemCache, …
├── cart/        корзина пользователя: CartItem, CartItemRepository, CartService, CartController, …
├── order/       заказы пользователя: Order, OrderItem, OrderRepository, OrderItemRepository, OrderService, …
├── purchase/    покупка: PurchaseService, PurchaseController
├── payment/     клиент сервиса платежей: PaymentClientConfig, ClientCredentialsExchangeFilter,
│                PaymentService, PaymentProperties, PaymentAvailability, исключения
├── image/       изображения товаров
├── itemimport/  загрузка товаров из CSV
└── common/      NotFoundException, EmptyCartException, GlobalExceptionHandler, ClockConfig
```

### Кеш товаров в Redis

| Ключ | Значение | Используется |
|---|---|---|
| `items:list` | JSON-список всех товаров: `id`, `title`, `description`, `price` | поиск, сортировка и пагинация витрины |
| `items:card:{id}` | JSON карточки: `id`, `title`, `description`, `imgPath`, `price` | карточка товара, плитки витрины, корзина, покупка |

- Время жизни — `market.cache.items-ttl`, по умолчанию 2 минуты.
- При промахе данные загружаются из БД и кладутся в Redis.
- После импорта товаров `items:list` сбрасывается.
- Если Redis недоступен, данные читаются из БД.

### Схема базы данных

```
users                   items                  cart_items                   orders               order_items
──────────────────      ─────────────────      ─────────────────────        ───────────────      ────────────────────
id        PK            id          PK         id       PK                  id         PK        id        PK
username  UNIQUE        title                  user_id  FK→users            user_id    FK→users  order_id  FK→orders
password  (BCrypt)      description            item_id  FK→items            created_at           item_id   FK→items
role      USER|ADMIN    img_path               quantity (> 0)               total_sum            title
                        price       (>= 0)     UNIQUE (user_id, item_id)                         price
                                                                                                 quantity  (> 0)
```

- Корзина и заказы привязаны к пользователю через `user_id`.
- В `order_items` название и цена копируются на момент покупки.
- Схема, начальный каталог и пользователи накатываются скриптами `schema.sql` и `data.sql` при старте.
- База в памяти (`r2dbc:h2:mem:///market`).

### Загрузка товаров

Страница http://localhost:8080/admin/items (только `admin`) принимает CSV-файл в UTF-8 с разделителем `;`
и файлы изображений:

```csv
title;price;image;description
Хоккейная шайба;350;puck.png;Официальная шайба
```

При ошибке в любой строке ни один товар не добавляется и ни одно изображение не сохраняется.

## Требования

- JDK 21;
- Docker — для Redis, запуска в контейнерах и интеграционных тестов витрины (Testcontainers).

Maven устанавливать не нужно — в проекте есть Maven Wrapper (`mvnw`).

## Сборка

```bash
./mvnw clean verify
```

Без тестов:

```bash
./mvnw clean package -DskipTests
```

Готовые Executable JAR: `market-app/target/my-market-app.jar`, `payment-service/target/payment-service.jar`,
`auth-server/target/auth-server.jar`.

## Тесты

```bash
./mvnw test
```

Интеграционным тестам витрины нужен запущенный Docker: Redis поднимается в контейнере Testcontainers.

### market-app

| Уровень | Инструменты | Что проверяется |
|---|---|---|
| Модульные | JUnit 5, Mockito, `StepVerifier` | сервисы с привязкой к пользователю, `MarketUserDetailsService`, `ClientCredentialsExchangeFilter` (Bearer-токен, сброс при `401`), разбор ошибок OAuth2 и сервиса платежей |
| Доступ к данным | `@DataR2dbcTest` | корзина и заказы разных пользователей, уникальность логина, ограничения БД |
| Веб-слой | `@WebFluxTest`, `WebTestClient`, Spring Security Test (`mockAuthentication`, `csrf`) | доступ анонимного пользователя, пользователя и администратора, скрытие кнопок в HTML, CSRF, выход с удалением кук |
| Интеграционные | `@SpringBootTest`, Redis в Testcontainers, MockWebServer | вход по форме с паролем из БД и выход, изоляция корзин и заказов, запросы в сервис платежей с токеном Client Credentials, кеширование в Redis |

`PaymentServerStub` на MockWebServer одновременно играет роль сервера авторизации и сервиса платежей:

- выдаёт токены на `/oauth2/token`, проверяя Basic-аутентификацию клиента, grant type и scope;
- принимает запросы к `/api/**` только с выданным им токеном;
- ведёт счета пользователей.

Так проверяется весь поток Client Credentials: запрос токена, его повторное использование, получение нового
токена после отказа и ошибка, если клиент не авторизован.

### payment-service

- **`AccountServiceTest`:** раздельные счета, списание, параллельные платежи.
- **`PaymentsApiControllerTest`:** `@WebFluxTest` сгенерированного контроллера с `mockJwt()`: `401` без токена,
  `403` без scope.
- **`PaymentServiceIntegrationTest`:** `@SpringBootTest` на случайном порту с настоящими JWT. Токены подписываются
  тестовым RSA-ключом, JWKS отдаёт MockWebServer. Проверяются чужой ключ, просроченный токен, чужой
  издатель, чужая аудитория и отсутствие scope.

### auth-server

- **`ResourceAudienceTokenCustomizerTest`:** аудитория в токене по выданным scope.
- **`AuthServerIntegrationTest`:** выдача токена по Client Credentials, его claims (`sub`, `aud`, `scope`,
  `iss`, срок жизни), отказ при неверном секрете, неизвестном клиенте, чужом scope и grant type, публикация
  JWKS и метаданных.

Для кеширования контекстов у каждого вида тестов витрины есть базовый класс в пакете `support` со всей
конфигурацией. За прогон витрины поднимаются три контекста Spring.

## Запуск локально

1. Запустить Redis:

   ```bash
   docker run -d --name market-redis -p 6379:6379 redis:7.4-alpine
   ```

2. Собрать проект:

   ```bash
   ./mvnw clean package -DskipTests
   ```

3. Запустить сервер авторизации, сервис платежей и витрину, каждый в своём терминале:

   ```bash
   java -jar auth-server/target/auth-server.jar
   ```

   ```bash
   java -jar payment-service/target/payment-service.jar
   ```

   ```bash
   java -jar market-app/target/my-market-app.jar
   ```

4. Открыть http://localhost:8080 и войти как `alice` / `alice123`.

Из IntelliJ IDEA: открыть корневой `pom.xml` как проект, выполнить `mvn generate-sources`, включить
annotation processing для Lombok и запустить `AuthServerApplication`, `PaymentServiceApplication`
и `MyMarketAppApplication`.

## Запуск в Docker Compose

```bash
docker compose up --build
```

- Витрина: http://localhost:8080
- Сервис платежей: http://localhost:8081 (API только с токеном, `/actuator/health` открыт)
- Сервер авторизации: http://localhost:9000/.well-known/oauth-authorization-server

У Redis, сервера авторизации и сервиса платежей есть healthcheck. Сервис платежей ждёт готовности сервера
авторизации, а витрина — готовности всех трёх сервисов. Внутри сети Compose издатель токенов —
`http://auth-server:9000`.

```bash
docker compose down
```
