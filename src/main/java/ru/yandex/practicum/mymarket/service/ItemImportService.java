package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.mymarket.exception.ItemImportException;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    public int importItems(MultipartFile csv, List<MultipartFile> images) {
        images.stream().filter(image -> !image.isEmpty()).forEach(imageService::store);
        try (InputStream in = csv.getInputStream()) {
            List<Item> items = parse(in);
            itemRepository.saveAll(items);
            return items.size();
        } catch (IOException e) {
            throw new ItemImportException("Не удалось прочитать файл " + csv.getOriginalFilename(), e);
        }
    }

    List<Item> parse(InputStream in) throws IOException {
        List<Item> items = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.strip();
            if (line.isEmpty() || (lineNumber == 1 && line.toLowerCase().startsWith("title" + SEPARATOR))) {
                continue;
            }
            items.add(parseLine(line, lineNumber));
        }
        return items;
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
