package ru.yandex.practicum.mymarket.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import ru.yandex.practicum.mymarket.exception.ItemImportException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageServiceTest {

    @TempDir
    private Path uploadDir;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(uploadDir);
    }

    @Test
    void store_savesFileAndReturnsImagePath() throws Exception {
        MockMultipartFile file = new MockMultipartFile("images", "photo.png", "image/png", new byte[]{1, 2, 3});

        String imgPath = imageService.store(file);

        assertThat(imgPath).isEqualTo("images/photo.png");
        assertThat(Files.readAllBytes(uploadDir.resolve("photo.png"))).containsExactly(1, 2, 3);
    }

    @Test
    void store_rejectsPathTraversal() {
        MockMultipartFile file = new MockMultipartFile("images", "../evil.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> imageService.store(file)).isInstanceOf(ItemImportException.class);
    }

    @Test
    void load_returnsUploadedImage() throws Exception {
        Files.write(uploadDir.resolve("uploaded.png"), new byte[]{7});

        Resource image = imageService.load("uploaded.png").orElseThrow();

        assertThat(image.getContentAsByteArray()).containsExactly(7);
    }

    @Test
    void load_fallsBackToBundledImage() {
        assertThat(imageService.load("ball.svg")).isPresent();
    }

    @Test
    void load_unknownOrInvalidName_returnsEmpty() {
        assertThat(imageService.load("missing.png")).isEmpty();
        assertThat(imageService.load("..")).isEmpty();
        assertThat(imageService.load(".hidden")).isEmpty();
    }
}
