package ru.yandex.practicum.mymarket.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;

@Controller
public class LoginController {

    public static final String ACCESS_DENIED_MESSAGE = "Недостаточно прав для доступа к этой странице";

    @GetMapping(SecurityConfig.LOGIN_PAGE)
    public String loginPage() {
        return "login";
    }

    @GetMapping(SecurityConfig.ACCESS_DENIED_URL)
    public Rendering accessDenied() {
        return Rendering.view("error")
                .modelAttribute("status", HttpStatus.FORBIDDEN.value())
                .modelAttribute("error", HttpStatus.FORBIDDEN.getReasonPhrase())
                .modelAttribute("message", ACCESS_DENIED_MESSAGE)
                .status(HttpStatus.FORBIDDEN)
                .build();
    }
}
