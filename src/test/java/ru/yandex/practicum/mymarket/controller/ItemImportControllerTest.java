package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.mymarket.exception.ItemImportException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ItemImportControllerTest extends ControllerTestBase {

    private static final MockMultipartFile CSV =
            new MockMultipartFile("file", "items.csv", "text/csv", "Мяч;100;;".getBytes());

    @Test
    void getImportPage_rendersForm() throws Exception {
        mockMvc.perform(get("/admin/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("import"));
    }

    @Test
    void importItems_success_redirectsWithMessage() throws Exception {
        MockMultipartFile image = new MockMultipartFile("images", "ball.png", "image/png", new byte[]{1});
        when(itemImportService.importItems(any(), anyList())).thenReturn(1);

        mockMvc.perform(multipart("/admin/items/import").file(CSV).file(image))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/items"))
                .andExpect(flash().attribute("message", "Добавлено товаров: 1"));

        verify(itemImportService).importItems(any(), argThat((List<MultipartFile> images) -> images.size() == 1));
    }

    @Test
    void importItems_invalidFile_redirectsWithError() throws Exception {
        when(itemImportService.importItems(any(), anyList())).thenThrow(new ItemImportException("Строка 1: ошибка"));

        mockMvc.perform(multipart("/admin/items/import").file(CSV))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Строка 1: ошибка"));
    }

    @Test
    void importItems_emptyFile_redirectsWithError() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "items.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/admin/items/import").file(empty))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        verifyNoInteractions(itemImportService);
    }
}
