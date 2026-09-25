# Витрина интернет-магазина и сервис платежей (my-market)

Мультипроект Maven из двух приложений на Spring Boot и реактивном стеке:

- **market-app** — веб-приложение «Витрина интернет-магазина» (Spring WebFlux + Thymeleaf,
  Spring Data R2DBC). Пользователь просматривает каталог, кладёт товары в корзину, оплачивает заказ
  через сервис платежей и смотрит историю заказов. Товары кешируются в Redis.
- **payment-service** — RESTful-сервис платежей на Spring WebFlux: отдаёт баланс счёта и списывает
  с него сумму заказа. Обмен данными — JSON.

Интеграция описана OpenAPI-спецификацией [`openapi/payment-api.yaml`](openapi/payment-api.yaml).
По ней при сборке генерируются HTTP-клиент для витрины и REST-контроллер для сервиса платежей.

## Стек

| Компонент | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.5 |
| Веб-слой | Spring WebFlux, встроенный Netty; Thymeleaf в витрине |
| Доступ к данным | Spring Data R2DBC, H2 в памяти (`r2dbc-h2`) |
| Кеш товаров | Redis, Spring Data Redis Reactive (`ReactiveRedisTemplate`, Lettuce) |
| Интеграция | OpenAPI 3, OpenAPI Generator 7 (генератор `spring`): клиент `spring-http-interface` поверх `WebClient`, сервер — реактивный контроллер с делегатом |
| Тесты | JUnit 5, Mockito, Reactor Test, Spring Boot Test (`@SpringBootTest`, `@WebFluxTest`, `@DataR2dbcTest`), `WebTestClient`, Testcontainers (Redis), MockWebServer |
| Сборка | Maven (Maven Wrapper), мультимодульный проект, Executable JAR |
| Развёртывание | Docker, Docker Compose |

## Структура мультипроекта

```
.
├── pom.xml                      корневой pom: модули, версии плагинов, общая конфигурация
├── openapi/payment-api.yaml     OpenAPI-спецификация сервиса платежей
├── docker-compose.yml           Redis + payment-service + market-app
├── market-app/                  витрина интернет-магазина (порт 8080)
│   ├── pom.xml                  генерация клиента по спецификации
│   ├── Dockerfile
│   └── src/
└── payment-service/             сервис платежей (порт 8081)
    ├── pom.xml                  генерация серверного кода по спецификации
    ├── Dockerfile
    └── src/
```

Сгенерированный код не хранится в репозитории: он появляется в `target/generated-sources/openapi`
на фазе `generate-sources` каждого модуля.

## Сервис платежей (payment-service)

### API

| Метод | Путь | Тело запроса | Ответ |
|---|---|---|---|
| GET | `/api/balance` | — | `200 {"amount": 50000}` |
| POST | `/api/payments` | `{"amount": 5600}` | `200 {"amount": 5600, "balance": 44400}` |
| | | | `409 {"code": "INSUFFICIENT_FUNDS", "message": "..."}` — недостаточно средств, платёж не проведён |
| | | | `400 {"code": "INVALID_REQUEST", "message": "..."}` — сумма не указана или не положительна |

Пример:

```bash
curl -s localhost:8081/api/balance
```

```bash
curl -s -H 'Content-Type: application/json' -d '{"amount":5600}' localhost:8081/api/payments
```

### Устройство

```
payment-service/src/main/java/ru/yandex/practicum/payment
├── PaymentServiceApplication.java
├── account/   AccountService — баланс счёта и атомарное списание,
│              AccountProperties, InsufficientFundsException
└── web/       PaymentsApiDelegateImpl — реализация сгенерированного делегата,
               ApiExceptionHandler — ошибки в формате ErrorResponse
```

Из спецификации генерируются `PaymentsApi` (интерфейс с аннотациями маршрутов и валидацией),
`PaymentsApiController` (REST-контроллер) и `PaymentsApiDelegate`, а также модели `Balance`,
`PaymentRequest`, `PaymentResult`, `ErrorResponse`. Логика подключается реализацией делегата.

Баланс хранится в памяти и задаётся свойством `payment.account.initial-balance` (по умолчанию 50 000 руб.).
Списание выполняется атомарно (`AtomicLong.compareAndSet`), поэтому параллельные платежи не уводят
баланс в минус. После перезапуска сервиса баланс восстанавливается.

## Витрина (market-app)

### Возможности

- **Витрина** (`/`, `/items`): плитка товаров по три в ряд, поиск по вхождению строки в название или описание,
  сортировка по алфавиту и по цене, пагинация по 2, 5, 10, 20, 50 и 100 товаров, изменение количества в корзине.
- **Карточка товара** (`/items/{id}`).
- **Корзина** (`/cart/items`): товары, количество, цены и общая сумма. Показывает баланс из сервиса платежей.
  Кнопка «Купить» доступна, только если баланса хватает на заказ. Если средств недостаточно или сервис
  платежей недоступен, вместо кнопки выводится сообщение.
- **Покупка** (`POST /buy`): заказ создаётся и оплачивается запросом в сервис платежей в одной реактивной
  транзакции. Если платёж отклонён (`409`) или сервис недоступен, заказ откатывается, корзина остаётся,
  выводится страница ошибки (`409` или `503`).
- **Заказы** (`/orders`, `/orders/{id}`).
- **Загрузка товаров** (`/admin/items`): импорт из CSV вместе с изображениями.

### Эндпоинты

| Метод | Путь | Описание | Результат |
|---|---|---|---|
| GET | `/`, `/items?search=&sort=NO\|ALPHA\|PRICE&pageNumber=1&pageSize=5` | Витрина | шаблон `items` |
| POST | `/items?id=&action=PLUS\|MINUS&search=&sort=&pageNumber=&pageSize=` | Изменить количество с витрины | `redirect:/items?...` |
| GET | `/items/{id}` | Карточка товара | шаблон `item` |
| POST | `/items/{id}?action=PLUS\|MINUS` | Изменить количество с карточки | шаблон `item` |
| GET | `/cart/items` | Корзина с проверкой баланса | шаблон `cart` |
| POST | `/cart/items?id=&action=PLUS\|MINUS\|DELETE` | Изменить количество / удалить из корзины | шаблон `cart` |
| POST | `/buy` | Оплатить и оформить заказ | `redirect:/orders/{id}?newOrder=true` |
| GET | `/orders` | Список заказов | шаблон `orders` |
| GET | `/orders/{id}?newOrder=false` | Страница заказа | шаблон `order` |
| GET | `/admin/items?imported=N` | Форма загрузки товаров | шаблон `import` |
| POST | `/admin/items/import` | Импорт CSV (`file`) и изображений (`images`) | `redirect:/admin/items?imported=N` |
| GET | `/images/{fileName}` | Изображение товара | файл изображения |

POST-эндпоинты принимают параметры как из строки запроса, так и из тела формы. Редиректы после POST
отдаются со статусом `303 See Other`.

### Структура

```
market-app/src/main/java/ru/yandex/practicum/mymarket
├── item/        товары: Item, ItemRepository, ItemService, ItemController, ItemDto, ItemMapper,
│                ItemGrid, Paging, SortType, CatalogCartForm, ItemCartForm;
│                кеш: ItemCache, ItemCacheConfig, ItemCacheProperties, ItemSummary, ItemCard
├── cart/        корзина: CartItem, CartItemRepository, CartService, CartController, CartDto, CartLine,
│                CartAction, CartItemForm
├── order/       заказы: Order, OrderItem, OrderRepository, OrderItemRepository, OrderService,
│                OrderController, OrderDto, OrderItemDto, OrderMapper
├── purchase/    покупка: PurchaseService (заказ + оплата + очистка корзины), PurchaseController
├── payment/     интеграция с сервисом платежей: PaymentClientConfig, PaymentService,
│                PaymentProperties, PaymentAvailability, PaymentRejectedException,
│                PaymentUnavailableException
├── image/       изображения товаров: ImageService, ImageController
├── itemimport/  загрузка товаров из CSV: ItemImportService, ItemImportController, ItemImportException
└── common/      NotFoundException, EmptyCartException, GlobalExceptionHandler, ClockConfig
```

### Интеграция с сервисом платежей

Из спецификации генерируется декларативный HTTP-интерфейс `PaymentsApi` (`@HttpExchange`, методы
возвращают `Mono`) и модели в пакете `ru.yandex.practicum.mymarket.payment.client`.
`PaymentClientConfig` создаёт его реализацию через `HttpServiceProxyFactory` поверх `WebClient`.

`PaymentService` оборачивает клиент:

- ограничивает время ответа таймаутом `market.payment.timeout` (по умолчанию 3 с);
- переводит ответ `409` в `PaymentRejectedException` с сообщением от сервиса платежей;
- переводит остальные ошибки (сервис не запущен, `5xx`, таймаут) в `PaymentUnavailableException`;
- для страницы корзины возвращает `PaymentAvailability`: можно ли оплатить заказ, баланс и сообщение.

Адрес сервиса задаётся свойством `market.payment.base-url` (по умолчанию `http://localhost:8081`).

### Кеш товаров в Redis

`ItemCache` хранит в Redis два вида данных с временем жизни `market.cache.items-ttl` (по умолчанию 2 минуты):

| Ключ | Значение | Используется |
|---|---|---|
| `items:list` | JSON-список всех товаров: `id`, `title`, `description`, `price` | поиск, сортировка и пагинация витрины |
| `items:card:{id}` | JSON карточки: `id`, `title`, `description`, `imgPath`, `price` | карточка товара, плитки витрины, корзина, покупка |

- Если данных в кеше нет, они загружаются из БД и записываются в Redis. Для плиток витрины и корзины карточки
  читаются одним `MGET`, из БД догружаются только отсутствующие.
- Витрина фильтрует, сортирует и режет на страницы список из `items:list`, а затем получает карточки
  товаров текущей страницы.
- После импорта товаров ключ `items:list` удаляется, чтобы новые товары сразу появились на витрине.
- Если Redis недоступен, ошибка записывается в лог, а данные читаются напрямую из БД.

### Схема базы данных

```
items                     cart_items                 orders                 order_items
─────────────────         ─────────────────          ─────────────          ─────────────────────
id          PK            id        PK               id          PK         id        PK
title                     item_id   FK→items, UNIQUE created_at             order_id  FK→orders
description               quantity  (> 0)            total_sum              item_id   FK→items
img_path                                                                    title
price       (>= 0)                                                          price
                                                                            quantity  (> 0)
```

- Корзина одна на приложение (пользователей нет).
- В `order_items` название и цена копируются на момент покупки, поэтому изменения каталога
  не меняют историю заказов.
- Схема и начальный каталог накатываются скриптами `schema.sql` и `data.sql` при старте.
- База в памяти (`r2dbc:h2:mem:///market`): после перезапуска корзина и заказы сбрасываются.

### Загрузка товаров

Страница http://localhost:8080/admin/items принимает CSV-файл в UTF-8 с разделителем `;` и файлы изображений:

```csv
title;price;image;description
Хоккейная шайба;350;puck.png;Официальная шайба
Клюшка;4200;;Деревянная клюшка
```

- `image` — имя загружаемого вместе со списком файла (может быть пустым);
- `description` — последнее поле, может содержать `;`;
- при ошибке в любой строке ни один товар не добавляется и ни одно изображение не сохраняется.

Изображения сохраняются в каталог `market.images.dir` (по умолчанию `./uploaded-images`,
в Docker — `/app/uploaded-images`), размер файла — до 10 МБ.

## Требования

- JDK 21;
- Docker — для Redis, запуска в контейнерах и интеграционных тестов витрины (Testcontainers).

Maven устанавливать не нужно — в проекте есть Maven Wrapper (`mvnw`).

## Сборка

Сборка всего мультипроекта с тестами (генерация кода по OpenAPI выполняется автоматически):

```bash
./mvnw clean verify
```

Без тестов:

```bash
./mvnw clean package -DskipTests
```

Отдельный модуль:

```bash
./mvnw -pl payment-service -am package
```

Готовые Executable JAR: `market-app/target/my-market-app.jar` и `payment-service/target/payment-service.jar`.

## Тесты

```bash
./mvnw test
```

Интеграционным тестам витрины нужен запущенный Docker: Redis поднимается в контейнере Testcontainers.

### market-app

| Уровень | Инструменты | Классы |
|---|---|---|
| Модульные | JUnit 5, Mockito, `StepVerifier` | `*ServiceTest`, `ItemCacheTest`, `ItemGridTest` |
| Доступ к данным | `@DataR2dbcTest` | `*RepositoryTest` |
| Веб-слой | `@WebFluxTest`, `WebTestClient` | `*ControllerTest` |
| Интеграционные | `@SpringBootTest`, `WebTestClient`, Redis в Testcontainers, MockWebServer | `*IntegrationTest`, `MyMarketAppApplicationTests` |

- `ItemCacheIntegrationTest` проверяет кеширование на настоящем Redis:
  - загрузку из БД при промахе и запись с TTL;
  - что витрина, карточка и корзина отдают данные из кеша после изменения БД;
  - частичные попадания в кеш и сброс кеша списка.
- `PaymentIntegrationTest` проверяет HTTP-запросы в сервис платежей: запрос баланса со страницы корзины,
  тело платежа при покупке, откат заказа при отказе (`409`) и при недоступности сервиса.
  Сервис платежей заменён заглушкой `PaymentServerStub` на MockWebServer: она ведёт баланс
  и записывает запросы.

Для кеширования контекстов у каждого вида тестов есть базовый класс в пакете `support`
(`RepositoryTestBase`, `ControllerTestBase`, `IntegrationTestBase`) со всей конфигурацией.
Наследники её не меняют, поэтому весь прогон поднимает три контекста Spring. Контейнер Redis и заглушка
сервиса платежей статические и живут всю JVM, чтобы закешированный контекст оставался рабочим.
Перед каждым интеграционным тестом Redis очищается, а после теста удаляются созданные им данные.

### payment-service

| Уровень | Классы |
|---|---|
| Модульные | `AccountServiceTest`: списание, нехватка средств, параллельные платежи |
| Веб-слой | `PaymentsApiControllerTest`: `@WebFluxTest` сгенерированного контроллера с реализацией делегата |
| Интеграционные | `PaymentServiceIntegrationTest`: `@SpringBootTest` на случайном порту |

## Запуск локально

1. Запустить Redis:

   ```bash
   docker run -d --name market-redis -p 6379:6379 redis:7.4-alpine
   ```

2. Собрать проект и запустить сервис платежей:

   ```bash
   ./mvnw clean package -DskipTests
   ```

   ```bash
   java -jar payment-service/target/payment-service.jar
   ```

3. В другом терминале запустить витрину:

   ```bash
   java -jar market-app/target/my-market-app.jar
   ```

4. Открыть http://localhost:8080.

Из IntelliJ IDEA: открыть корневой `pom.xml` как проект, выполнить `mvn generate-sources` (или Build),
включить annotation processing для Lombok и запустить `PaymentServiceApplication` и `MyMarketAppApplication`.

Порты и адреса меняются параметрами, например
`--server.port=9090 --market.payment.base-url=http://localhost:8081 --spring.data.redis.host=localhost`.

## Запуск в Docker Compose

Собрать образы и запустить Redis, сервис платежей и витрину:

```bash
docker compose up --build
```

- Витрина: http://localhost:8080
- Сервис платежей: http://localhost:8081/api/balance

Остановить и удалить контейнеры:

```bash
docker compose down
```

Контейнеры можно запускать и по отдельности. Образы собираются из корня проекта:

```bash
docker build -f payment-service/Dockerfile -t payment-service .
```

```bash
docker build -f market-app/Dockerfile -t my-market-app .
```
