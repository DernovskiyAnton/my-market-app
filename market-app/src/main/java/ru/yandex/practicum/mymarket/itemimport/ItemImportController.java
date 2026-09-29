package ru.yandex.practicum.mymarket.itemimport;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/admin/items")
@RequiredArgsConstructor
public class ItemImportController {

    public static final String IMPORT_VIEW = "import";

    private final ItemImportService itemImportService;

    @GetMapping
    public Rendering getImportPage(@RequestParam(required = false) Integer imported) {
        Rendering.Builder<?> rendering = Rendering.view(IMPORT_VIEW);
        if (imported != null) {
            rendering.modelAttribute("message", "Добавлено товаров: " + imported);
        }
        return rendering.build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> importItems(@RequestPart("file") FilePart file,
                                    @RequestPart(value = "images", required = false) Flux<FilePart> images) {
        return images.collectList()
                .flatMap(imageList -> itemImportService.importItems(file, imageList))
                .map(imported -> "redirect:/admin/items?imported=" + imported);
    }
}
