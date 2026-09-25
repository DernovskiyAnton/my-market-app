package ru.yandex.practicum.mymarket.itemimport;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.image.ImageService;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemRepository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemImportService {

    private static final String SEPARATOR = ";";
    private static final int FIELDS_COUNT = 4;

    private final ItemRepository itemRepository;
    private final ImageService imageService;

    @Transactional
    public Mono<Integer> importItems(FilePart csv, List<FilePart> images) {
        return readContent(csv)
                .map(this::parse)
                .flatMap(items -> itemRepository.saveAll(items)
                        .then(storeImages(images))
                        .thenReturn(items.size()));
    }

    List<Item> parse(String content) {
        if (content.isBlank()) {
            throw new ItemImportException("Выберите CSV-файл со списком товаров");
        }
        List<Item> items = new ArrayList<>();
        List<String> lines = content.lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i).strip();
            if (line.isEmpty() || (lineNumber == 1 && line.toLowerCase().startsWith("title" + SEPARATOR))) {
                continue;
            }
            items.add(parseLine(line, lineNumber));
        }
        return items;
    }

    private Mono<String> readContent(FilePart csv) {
        return DataBufferUtils.join(csv.content())
                .map(buffer -> {
                    String content = buffer.toString(StandardCharsets.UTF_8);
                    DataBufferUtils.release(buffer);
                    return content;
                })
                .defaultIfEmpty("");
    }

    private Mono<Void> storeImages(List<FilePart> images) {
        return Flux.fromIterable(images)
                .filter(image -> StringUtils.hasText(image.filename()))
                .concatMap(image -> imageService.store(image.filename(), image.content()))
                .then();
    }

    private Item parseLine(String line, int lineNumber) {
        String[] fields = line.split(SEPARATOR, FIELDS_COUNT);
        if (fields.length < FIELDS_COUNT - 1) {
            throw new ItemImportException("Строка " + lineNumber + ": ожидается формат title;price;image;description");
        }
        String title = fields[0].strip();
        if (title.isEmpty()) {
            throw new ItemImportException("Строка " + lineNumber + ": не указано название товара");
        }
        long price = parsePrice(fields[1].strip(), lineNumber);
        String image = fields[2].strip();
        String description = fields.length == FIELDS_COUNT ? fields[3].strip() : "";
        String imgPath = StringUtils.hasText(image) ? ImageService.IMAGE_PATH_PREFIX + image : null;
        return new Item(title, description, imgPath, price);
    }

    private static long parsePrice(String value, int lineNumber) {
        try {
            long price = Long.parseLong(value);
            if (price < 0) {
                throw new ItemImportException("Строка " + lineNumber + ": цена не может быть отрицательной");
            }
            return price;
        } catch (NumberFormatException e) {
            throw new ItemImportException("Строка " + lineNumber + ": некорректная цена '" + value + "'", e);
        }
    }
}
