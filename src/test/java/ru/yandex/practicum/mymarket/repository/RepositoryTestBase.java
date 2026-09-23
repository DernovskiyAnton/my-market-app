package ru.yandex.practicum.mymarket.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Общая конфигурация тестов слоя доступа к данным. Все наследники используют
 * одинаковую конфигурацию, поэтому контекст Spring создаётся один раз и берётся из кеша.
 * Схема и начальный каталог (12 товаров) накатываются из schema.sql и data.sql.
 */
@DataJpaTest
abstract class RepositoryTestBase {

    @Autowired
    protected TestEntityManager entityManager;
}
