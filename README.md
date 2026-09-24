# Витрина интернет-магазина (my-market-app)

Веб-приложение «Витрина интернет-магазина» на Spring Boot и блокирующем стеке:
пользователь просматривает каталог товаров, кладёт товары в корзину, оформляет заказ
и смотрит историю заказов.

## Стек

| Компонент | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.5 |
| Веб-слой | Spring Web MVC + Thymeleaf, встроенный Tomcat |
| Доступ к данным | Spring Data JPA, Hibernate ORM |
| База данных | H2 в памяти |
| Тесты | JUnit 5, Mockito, Spring TestContext Framework, Spring Boot Test (`@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`) |
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
| POST | `/admin/items/import` | Импорт CSV (`file`) и изображений (`images`) | `redirect:/admin/items` |
| GET | `/images/{fileName}` | Изображение товара | файл изображения |

Несуществующие товар или заказ — `404 Not Found`, покупка с пустой корзиной и некорректные
параметры запроса — `400 Bad Request`.

## Структура проекта

Код разложен по модулям (частям) приложения, внутри каждого модуля — свои слои:
контроллер, сервис, репозиторий, сущности, DTO и мапперы.

```
src/main/java/ru/yandex/practicum/mymarket
├── MyMarketAppApplication.java  точка входа
├── item/        товары: Item, ItemRepository, ItemService, ItemController,
│                ItemDto, ItemMapper, ItemGrid, Paging, SortType
├── cart/        корзина: CartItem, CartItemRepository, CartService, CartController,
│                CartDto, CartAction
├── order/       заказы: Order, OrderItem, OrderRepository, OrderService, OrderController,
│                OrderDto, OrderItemDto, OrderMapper
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
например, `PurchaseService` берёт позиции из `CartService` и сохраняет заказ через `OrderService`.

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
- Схема и данные накатываются скриптами `schema.sql` и `data.sql` при старте
  (`spring.sql.init.mode=always`), Hibernate только проверяет соответствие сущностей схеме
  (`spring.jpa.hibernate.ddl-auto=validate`).
- База в памяти, поэтому после перезапуска приложения корзина и заказы сбрасываются.
  Консоль H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:market`, пользователь `sa`, без пароля).

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
- первая строка-заголовок пропускается; при ошибке в любой строке ни один товар не добавляется.

Загруженные изображения сохраняются в каталог из свойства `market.images.dir`
(по умолчанию `./uploaded-images`, в Docker — `/app/uploaded-images`).

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
| Модульные тесты сервисов | JUnit 5, Mockito | `*ServiceTest`, `ItemGridTest` |
| Слой доступа к данным | `@DataJpaTest` | `*RepositoryTest` |
| Веб-слой | `@WebMvcTest`, MockMvc, моки сервисов | `*ControllerTest` |
| Интеграционные | `@SpringBootTest`, `@AutoConfigureMockMvc` | `*IntegrationTest`, `MyMarketAppApplicationTests` |

Тесты лежат в тех же модулях, что и тестируемый код (`item/`, `cart/`, `order/`, `purchase/`,
`image/`, `itemimport/`). Сквозной сценарий «витрина → корзина → покупка → заказ» —
`MarketFlowIntegrationTest` в корневом пакете.

Для максимального переиспользования контекстов у каждого вида тестов есть базовый класс
в пакете `support` (`RepositoryTestBase`, `ControllerTestBase`, `IntegrationTestBase`), в котором собрана вся
конфигурация, включая все `@MockitoBean`. Наследники не добавляют своей конфигурации, поэтому
Spring TestContext Framework создаёт всего три контекста на весь прогон и берёт их из кеша.
Интеграционные тесты помечены `@Transactional` и откатывают изменения после каждого теста.

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
