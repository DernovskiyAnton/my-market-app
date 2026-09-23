package ru.yandex.practicum.mymarket.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
abstract class RepositoryTestBase {

    @Autowired
    protected TestEntityManager entityManager;
}
