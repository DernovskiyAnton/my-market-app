package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest extends ControllerTestBase {

    @Test
    void getImage_returnsImageWithContentType() throws Exception {
        when(imageService.load("ball.png")).thenReturn(Optional.of(new ByteArrayResource(new byte[]{1, 2, 3})));

        mockMvc.perform(get("/images/ball.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void getImage_missing_returnsNotFound() throws Exception {
        when(imageService.load("missing.png")).thenReturn(Optional.empty());

        mockMvc.perform(get("/images/missing.png"))
                .andExpect(status().isNotFound());
    }
}
