package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.mymarket.exception.ItemImportException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Хранение и выдача изображений товаров.
 * Загруженные изображения сохраняются в каталог {@code market.images.dir};
 * изображения начального каталога лежат в classpath в директории {@code images/}.
 */
@Service
public class ImageService {

    /** Префикс пути к изображению, который хранится в товаре и используется в URL. */
    public static final String IMAGE_PATH_PREFIX = "images/";

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[\\w-][\\w.-]*");

    private final Path uploadDir;

    public ImageService(@Value("${market.images.dir}") Path uploadDir) {
        this.uploadDir = uploadDir.toAbsolutePath().normalize();
    }

    public Optional<Resource> load(String fileName) {
        if (!isValidFileName(fileName)) {
            return Optional.empty();
        }
        Resource uploaded = new PathResource(uploadDir.resolve(fileName));
        if (uploaded.isReadable()) {
            return Optional.of(uploaded);
        }
        Resource bundled = new ClassPathResource(IMAGE_PATH_PREFIX + fileName);
        return bundled.exists() ? Optional.of(bundled) : Optional.empty();
    }

    /**
     * Сохраняет изображение под его исходным именем.
     *
     * @return путь к изображению для хранения в товаре, например {@code images/ball.jpg}
     */
    public String store(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (!isValidFileName(fileName)) {
            throw new ItemImportException("Недопустимое имя файла изображения: " + fileName);
        }
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(uploadDir);
            Files.copy(in, uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ItemImportException("Не удалось сохранить изображение " + fileName, e);
        }
        return IMAGE_PATH_PREFIX + fileName;
    }

    private static boolean isValidFileName(String fileName) {
        return fileName != null && FILE_NAME_PATTERN.matcher(fileName).matches();
    }
}
