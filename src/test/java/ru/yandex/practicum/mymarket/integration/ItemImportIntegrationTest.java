package ru.yandex.practicum.mymarket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemImportIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ItemService itemService;

    @Test
    void importItems_addsItemsToCatalogAndStoresImages() throws Exception {
        MockMultipartFile csv = new MockMultipartFile("file", "items.csv", "text/csv", """
                title;price;image;description
                Хоккейная шайба;350;puck.png;Официальная шайба
                Клюшка;4200;;Деревянная клюшка
                """.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile image = new MockMultipartFile("images", "puck.png", "image/png", new byte[]{9, 8, 7});

        mockMvc.perform(multipart("/admin/items/import").file(csv).file(image))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Добавлено товаров: 2"));

        Page<ItemDto> found = itemService.findItems("шайба", SortType.NO, 1, 10);
        assertThat(found.getContent()).singleElement()
                .extracting(ItemDto::title, ItemDto::price, ItemDto::imgPath)
                .containsExactly("Хоккейная шайба", 350L, "images/puck.png");
        mockMvc.perform(get("/images/puck.png"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{9, 8, 7}));
    }

    @Test
    void importItems_invalidCsv_doesNotChangeCatalog() throws Exception {
        long before = itemService.findItems("", SortType.NO, 1, 100).getTotalElements();
        MockMultipartFile csv = new MockMultipartFile("file", "items.csv", "text/csv",
                "Товар;100;;ок\nПлохой;не число;;".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/admin/items/import").file(csv))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertThat(itemService.findItems("", SortType.NO, 1, 100).getTotalElements()).isEqualTo(before);
    }
}
