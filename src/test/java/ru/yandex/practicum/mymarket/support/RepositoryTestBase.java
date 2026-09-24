package ru.yandex.practicum.mymarket.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
public abstract class RepositoryTestBase {

    @Autowired
    protected TestEntityManager entityManager;
}
