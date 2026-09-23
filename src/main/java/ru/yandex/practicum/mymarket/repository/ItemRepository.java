package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.mymarket.model.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Поиск товаров по вхождению строки в название или описание без учёта регистра.
     */
    @Query("""
            select i from Item i
            where lower(i.title) like lower(concat('%', :search, '%'))
               or lower(i.description) like lower(concat('%', :search, '%'))
            """)
    Page<Item> search(@Param("search") String search, Pageable pageable);
}
