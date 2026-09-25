# Витрина интернет-магазина (my-market-app)

Веб-приложение «Витрина интернет-магазина» на Spring Boot и реактивном стеке (Spring WebFlux + Spring Data R2DBC):
пользователь просматривает каталог товаров, кладёт товары в корзину, оформляет заказ
и смотрит историю заказов.

## Стек

| Компонент | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.5 |
| Веб-слой | Spring WebFlux + Thymeleaf, встроенный Netty |
| Доступ к данным | Spring Data R2DBC, реактивные транзакции (`R2dbcTransactionManager`) |
| База данных | H2 в памяти, реактивный драйвер `r2dbc-h2` |
| Реактивная библиотека | Project Reactor (`Mono`, `Flux`) |
| Тесты | JUnit 5, Mockito, Reactor Test (`StepVerifier`), Spring TestContext Framework, Spring Boot Test (`@SpringBootTest`, `@WebFluxTest`, `@DataR2dbcTest`), `WebTestClient` |
| Сборка | Maven (Maven Wrapper), Executable JAR |
| Развёртывание | Docker |

## Возможности

- **Витрина** (`/`, `/items`) — плитка товаров по три в ряд, поиск по вхождению строки
  в название или описание, сортировка по алфавиту и по цене, пагинация по 2, 5, 10, 20, 50, 100 товаров,
  добавление в корзину и изменение количества прямо с витрины.
- **Карточка товара** (`/items/{id}`) — название, изображение, описание, цена, управление количеством в корзине.
- **Корзина** (`/cart/items`) — список товаров с количеством и ценой, общая сумма,
  изменение количества, удаление товара, кнопка «Купить».
- **Покупка** (`POST /buy`) — эмуляция оформления заказа: из корзины создаётся заказ, корзина очищается,
  выполняется переход на страницу заказа.
- **Заказы** (`/orders`, `/orders/{id}`) — список всех заказов с товарами и суммами, страница отдельного заказа.
- **Загрузка товаров** (`/admin/items`) — импорт списка товаров из CSV-файла вместе с изображениями.
- **Изображения** (`/images/{fileName}`) — выдача изображений товаров.

## Эндпоинты

| Метод | Путь | Описание | Результат |
|---|---|---|---|
| GET | `/`, `/items?search=&sort=NO\|ALPHA\|PRICE&pageNumber=1&pageSize=5` | Витрина | шаблон `items` |
| POST | `/items?id=&action=PLUS\|MINUS&search=&sort=&pageNumber=&pageSize=` | Изменить количество в корзине с витрины | `redirect:/items?...` |
| GET | `/items/{id}` | Карточка товара | шаблон `item` |
| POST | `/items/{id}?action=PLUS\|MINUS` | Изменить количество в корзине с карточки | шаблон `item` |
| GET | `/cart/items` | Корзина | шаблон `cart` |
| POST | `/cart/items?id=&action=PLUS\|MINUS\|DELETE` | Изменить количество / удалить из корзины | шаблон `cart` |
| POST | `/buy` | Оформить заказ | `redirect:/orders/{id}?newOrder=true` |
| GET | `/orders` | Список заказов | шаблон `orders` |
| GET | `/orders/{id}?newOrder=false` | Страница заказа | шаблон `order` |
| GET | `/admin/items` | Форма загрузки товаров | шаблон `import` |
| GET | `/admin/items?imported=N` | Форма загрузки с сообщением о результате | шаблон `import` |
| POST | `/admin/items/import` | Импорт CSV (`file`) и изображений (`images`) | `redirect:/admin/items?imported=N` |
| GET | `/images/{fileName}` | Изображение товара | файл изображения |

POST-эндпоинты принимают параметры как из строки запроса, так и из тела формы
(`application/x-www-form-urlencoded`), которое отправляют шаблоны.

Несуществующие товар или заказ — `404 Not Found`, покупка с пустой корзиной и некорректные
параметры запроса — `400 Bad Request`, ошибка в CSV — страница загрузки с сообщением и `400`,
слишком большой файл — `413 Payload Too Large`. Редиректы после POST отдаются со статусом `303 See Other`.

## Структура проекта

Код разложен по модулям (частям) приложения, внутри каждого модуля — свои слои:
контроллер, сервис, репозиторий, сущности, DTO и мапперы. Все слои реактивные: репозитории
возвращают `Mono`/`Flux`, сервисы собирают из них цепочки, контроллеры возвращают `Mono<Rendering>`
или `Mono<String>` с редиректом, и ни один вызов не блокирует поток Netty.

```
src/main/java/ru/yandex/practicum/mymarket
├── MyMarketAppApplication.java  точка входа
├── item/        товары: Item, ItemRepository, ItemService, ItemController,
│                ItemDto, ItemMapper, ItemGrid, Paging, SortType, CatalogCartForm, ItemCartForm
├── cart/        корзина: CartItem, CartItemRepository, CartService, CartController,
│                CartDto, CartLine, CartAction, CartItemForm
├── order/       заказы: Order, OrderItem, OrderRepository, OrderItemRepository, OrderService,
│                OrderController, OrderDto, OrderItemDto, OrderMapper
├── purchase/    покупка: PurchaseService (оформляет заказ из корзины и очищает её),
│                PurchaseController (POST /buy)
├── image/       изображения товаров: ImageService, ImageController
├── itemimport/  загрузка товаров из CSV: ItemImportService, ItemImportController,
│                ItemImportException
└── common/      общее: NotFoundException, EmptyCartException, GlobalExceptionHandler, ClockConfig

src/main/resources
├── application.properties
├── schema.sql     схема БД
├── data.sql       начальный каталог (12 товаров)
├── images/        изображения начального каталога
└── templates/     Thymeleaf-шаблоны страниц
```

Контроллеры работают только с сервисами и DTO, сервисы — с репозиториями и сущностями,
в шаблоны передаются неизменяемые DTO (`record`). Модули обращаются друг к другу через сервисы:
например, `PurchaseService` берёт позиции из `CartService` и сохраняет заказ через `OrderService`
в одной реактивной транзакции.

Особенности реактивной реализации:

- В WebFlux `@RequestParam` читает только строку запроса, поэтому данные POST-форм привязываются
  через `@ModelAttribute` к record-классам (`CatalogCartForm`, `ItemCartForm`, `CartItemForm`)
  с проверкой через Bean Validation.
- В WebFlux нет flash-атрибутов, поэтому результат импорта передаётся на страницу загрузки
  параметром `imported`, а ошибка импорта сразу отображается на странице загрузки.
- Файлы загружаются как `FilePart` и пишутся на диск неблокирующим `DataBufferUtils.write`;
  чтение изображений с диска вынесено на `Schedulers.boundedElastic()`.
- В R2DBC нет связей между сущностями, поэтому сущности ссылаются друг на друга по идентификаторам
  (`itemId`, `orderId`), а позиции корзины и заказов собираются в сервисах запросами по списку
  идентификаторов, без N+1.

## Схема базы данных

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

- Корзина одна на приложение (пользователей нет): каждая строка `cart_items` — товар и его количество.
- В `order_items` название и цена товара копируются на момент покупки, поэтому последующие
  изменения каталога не меняют историю заказов; `total_sum` хранится в заказе.
- Схема и данные накатываются скриптами `schema.sql` и `data.sql` при старте через R2DBC
  (`spring.sql.init.mode=always`).
- База в памяти (`r2dbc:h2:mem:///market`), поэтому после перезапуска приложения корзина
  и заказы сбрасываются.

## Загрузка товаров

Страница http://localhost:8080/admin/items (кнопка «Загрузить товары» на витрине) принимает CSV-файл
в UTF-8 с разделителем `;` и, опционально, файлы изображений:

```csv
title;price;image;description
Хоккейная шайба;350;puck.png;Официальная шайба
Клюшка;4200;;Деревянная клюшка
```

- `image` — имя файла изображения из загружаемых вместе со списком (может быть пустым);
- `description` — последнее поле, может содержать `;`;
- первая строка-заголовок пропускается; при ошибке в любой строке ни один товар не добавляется
  и ни одно изображение не сохраняется: файлы пишутся только после успешного сохранения товаров.

Загруженные изображения сохраняются в каталог из свойства `market.images.dir`
(по умолчанию `./uploaded-images`, в Docker — `/app/uploaded-images`). Размер одного файла
ограничен 10 МБ (`spring.webflux.multipart.max-disk-usage-per-part`).

## Требования

- JDK 21 (для сборки и запуска без Docker);
- Docker (для запуска в контейнере).

Maven устанавливать не нужно — в проекте есть Maven Wrapper (`mvnw`).

## Запуск в среде разработки

1. Открыть проект в IntelliJ IDEA как Maven-проект (File → Open → `pom.xml` → Open as Project).
2. Указать Project SDK — Java 21, включить annotation processing (для Lombok).
3. Запустить класс `ru.yandex.practicum.mymarket.MyMarketAppApplication`.
4. Открыть http://localhost:8080.

Или из командной строки:

```bash
./mvnw spring-boot:run
```

## Тесты

```bash
./mvnw test
```

| Уровень | Инструменты | Классы |
|---|---|---|
| Модульные тесты сервисов | JUnit 5, Mockito, `StepVerifier` | `*ServiceTest`, `ItemGridTest` |
| Слой доступа к данным | `@DataR2dbcTest`, `StepVerifier` | `*RepositoryTest` |
| Веб-слой | `@WebFluxTest`, `WebTestClient`, моки сервисов | `*ControllerTest` |
| Интеграционные | `@SpringBootTest`, `@AutoConfigureWebTestClient` | `*IntegrationTest`, `MyMarketAppApplicationTests` |

Тесты лежат в тех же модулях, что и тестируемый код (`item/`, `cart/`, `order/`, `purchase/`,
`image/`, `itemimport/`). Сквозной сценарий «витрина → корзина → покупка → заказ» —
`MarketFlowIntegrationTest` в корневом пакете.

Для максимального переиспользования контекстов у каждого вида тестов есть базовый класс
в пакете `support` (`RepositoryTestBase`, `ControllerTestBase`, `IntegrationTestBase`), в котором собрана вся
конфигурация, включая все `@MockitoBean`. Наследники не добавляют своей конфигурации, поэтому
Spring TestContext Framework создаёт всего три контекста на весь прогон и берёт их из кеша.

Реактивные тесты не откатывают транзакции автоматически, поэтому данные очищаются явно:

- `RepositoryTestBase` перед каждым тестом очищает таблицы, и каждый тест репозитория создаёт
  свои данные. Тесты репозиториев работают с отдельной базой `repository-tests`, чтобы не задевать
  базу интеграционных тестов в той же JVM.
- `IntegrationTestBase` после каждого теста очищает корзину и заказы и удаляет товары,
  созданные тестом через `createItem(...)`. Стартовый каталог из `data.sql` при этом не меняется,
  и тесты от него не зависят.

## Сборка и локальный запуск Executable JAR

```bash
./mvnw clean package
```

```bash
java -jar target/my-market-app.jar
```

Приложение будет доступно на http://localhost:8080. Порт можно поменять: `--server.port=9090`.

## Запуск в Docker

Сборка образа (JAR собирается внутри многоэтапного Dockerfile, локальные JDK и Maven не нужны):

```bash
docker build -t my-market-app .
```

Запуск контейнера:

```bash
docker run --rm -p 8080:8080 --name my-market-app my-market-app
```

Приложение будет доступно на http://localhost:8080.

Чтобы загруженные изображения сохранялись между перезапусками контейнера, подключите том:

```bash
docker run --rm -p 8080:8080 -v market-images:/app/uploaded-images my-market-app
```
