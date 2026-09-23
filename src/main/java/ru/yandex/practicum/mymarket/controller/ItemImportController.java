package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.exception.ItemImportException;
import ru.yandex.practicum.mymarket.service.ItemImportService;

import java.util.List;

@Controller
@RequestMapping("/admin/items")
@RequiredArgsConstructor
public class ItemImportController {

    private final ItemImportService itemImportService;

    @GetMapping
    public String getImportPage() {
        return "import";
    }

    @PostMapping("/import")
    public String importItems(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "images", required = false) List<MultipartFile> images,
                              RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Выберите CSV-файл со списком товаров");
            return "redirect:/admin/items";
        }
        try {
            int imported = itemImportService.importItems(file, images == null ? List.of() : images);
            redirectAttributes.addFlashAttribute("message", "Добавлено товаров: " + imported);
        } catch (ItemImportException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/items";
    }
}
