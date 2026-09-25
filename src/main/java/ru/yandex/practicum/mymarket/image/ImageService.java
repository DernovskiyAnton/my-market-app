package ru.yandex.practicum.mymarket.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.mymarket.itemimport.ItemImportException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ImageService {

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
