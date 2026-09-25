package ru.yandex.practicum.mymarket.item;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ItemRepository extends R2dbcRepository<Item, Long> {

    Flux<Item> findAllBy(Pageable pageable);

    Flux<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description,
                                                                              Pageable pageable);

    Mono<Long> countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    default Flux<Item> search(String search, Pageable pageable) {
        return findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
    }

    default Mono<Long> countSearch(String search) {
        return countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);
    }
}
