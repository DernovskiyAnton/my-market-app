package ru.yandex.practicum.mymarket.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Общая конфигурация интеграционных тестов на полном контексте приложения.
 * Одинаковая конфигурация у всех наследников позволяет переиспользовать
 * закешированный контекст. Каждый тест выполняется в транзакции, которая
 * откатывается после его завершения, поэтому тесты не влияют друг на друга.
 */
@SpringBootTest(properties = "market.images.dir=target/test-images")
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;
}
