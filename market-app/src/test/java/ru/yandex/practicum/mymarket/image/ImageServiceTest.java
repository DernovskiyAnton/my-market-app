package ru.yandex.practicum.mymarket.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.itemimport.ItemImportException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImageServiceTest {

    @TempDir
    private Path uploadDir;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(uploadDir);
    }

    @Test
    void store_writesFileAndReturnsImagePath() throws Exception {
        StepVerifier.create(imageService.store("photo.png", content(1, 2, 3)))
                .expectNext("images/photo.png")
                .verifyComplete();

        assertThat(Files.readAllBytes(uploadDir.resolve("photo.png"))).containsExactly(1, 2, 3);
    }

    @Test
    void store_createsMissingDirectory() {
        ImageService service = new ImageService(uploadDir.resolve("nested"));

        StepVerifier.create(service.store("photo.png", content(1)))
                .expectNext("images/photo.png")
                .verifyComplete();

        assertThat(uploadDir.resolve("nested").resolve("photo.png")).exists();
    }

    @Test
    void store_rejectsPathTraversal() {
        StepVerifier.create(imageService.store("../evil.png", content(1)))
                .expectError(ItemImportException.class)
                .verify();
        assertThat(uploadDir.getParent().resolve("evil.png")).doesNotExist();
    }

    @Test
    void load_returnsUploadedImage() throws Exception {
        Files.write(uploadDir.resolve("uploaded.png"), new byte[]{7});

        StepVerifier.create(imageService.load("uploaded.png"))
                .assertNext(image -> assertThat(image.getFilename()).isEqualTo("uploaded.png"))
                .verifyComplete();
    }

    @Test
    void load_fallsBackToBundledImage() {
        StepVerifier.create(imageService.load("ball.svg"))
                .assertNext(image -> assertThat(image.exists()).isTrue())
                .verifyComplete();
    }

    @Test
    void load_unknownOrInvalidName_returnsEmpty() {
        StepVerifier.create(imageService.load("missing.png")).verifyComplete();
        StepVerifier.create(imageService.load("..")).verifyComplete();
        StepVerifier.create(imageService.load(".hidden")).verifyComplete();
    }

    private static Flux<DataBuffer> content(int... bytes) {
        byte[] data = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            data[i] = (byte) bytes[i];
        }
        return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(data));
    }
}
