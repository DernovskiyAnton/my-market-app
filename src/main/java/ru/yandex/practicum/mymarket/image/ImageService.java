package ru.yandex.practicum.mymarket.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.yandex.practicum.mymarket.itemimport.ItemImportException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@Service
public class ImageService {

    public static final String IMAGE_PATH_PREFIX = "images/";

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[\\w-][\\w.-]*");

    private final Path uploadDir;

    public ImageService(@Value("${market.images.dir}") Path uploadDir) {
        this.uploadDir = uploadDir.toAbsolutePath().normalize();
    }

    public Mono<Resource> load(String fileName) {
        if (!isValidFileName(fileName)) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> findImage(fileName))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<String> store(String fileName, Flux<DataBuffer> content) {
        if (!isValidFileName(fileName)) {
            return Mono.error(new ItemImportException("Недопустимое имя файла изображения: " + fileName));
        }
        return Mono.fromCallable(() -> Files.createDirectories(uploadDir))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(dir -> DataBufferUtils.write(content, dir.resolve(fileName)))
                .onErrorMap(IOException.class,
                        e -> new ItemImportException("Не удалось сохранить изображение " + fileName, e))
                .thenReturn(IMAGE_PATH_PREFIX + fileName);
    }

    private Resource findImage(String fileName) {
        Resource uploaded = new PathResource(uploadDir.resolve(fileName));
        if (uploaded.isReadable()) {
            return uploaded;
        }
        Resource bundled = new ClassPathResource(IMAGE_PATH_PREFIX + fileName);
        return bundled.exists() ? bundled : null;
    }

    private static boolean isValidFileName(String fileName) {
        return fileName != null && FILE_NAME_PATTERN.matcher(fileName).matches();
    }
}
