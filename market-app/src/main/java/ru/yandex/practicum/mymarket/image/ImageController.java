package ru.yandex.practicum.mymarket.image;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/images/{fileName}")
    public Mono<ResponseEntity<Resource>> getImage(@PathVariable String fileName) {
        return imageService.load(fileName)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(fileName)
                                .orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                        .body(image))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
