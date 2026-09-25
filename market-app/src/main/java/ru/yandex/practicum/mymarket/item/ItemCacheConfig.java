package ru.yandex.practicum.mymarket.item;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class ItemCacheConfig {

    @Bean
    public ReactiveRedisTemplate<String, ItemCard> itemCardRedisTemplate(ReactiveRedisConnectionFactory connectionFactory,
                                                                        ObjectMapper objectMapper) {
        return template(connectionFactory, new Jackson2JsonRedisSerializer<>(objectMapper, ItemCard.class));
    }

    @Bean
    public ReactiveRedisTemplate<String, List<ItemSummary>> itemListRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, ItemSummary.class);
        return template(connectionFactory, new Jackson2JsonRedisSerializer<List<ItemSummary>>(objectMapper, listType));
    }

    private static <T> ReactiveRedisTemplate<String, T> template(ReactiveRedisConnectionFactory connectionFactory,
                                                                 Jackson2JsonRedisSerializer<T> valueSerializer) {
        RedisSerializationContext<String, T> context = RedisSerializationContext
                .<String, T>newSerializationContext(new StringRedisSerializer())
                .value(valueSerializer)
                .build();
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
