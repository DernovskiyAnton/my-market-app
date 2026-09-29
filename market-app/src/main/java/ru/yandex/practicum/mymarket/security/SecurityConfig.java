package ru.yandex.practicum.mymarket.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.HeaderWriterServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.header.ClearSiteDataServerHttpHeadersWriter;

import java.net.URI;

import static org.springframework.security.web.server.header.ClearSiteDataServerHttpHeadersWriter.Directive.CACHE;
import static org.springframework.security.web.server.header.ClearSiteDataServerHttpHeadersWriter.Directive.COOKIES;
import static org.springframework.security.web.server.header.ClearSiteDataServerHttpHeadersWriter.Directive.STORAGE;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    public static final String LOGIN_PAGE = "/login";
    public static final String LOGIN_REQUIRED_URL = "/login?required";
    public static final String LOGOUT_SUCCESS_URL = "/items?logout";
    public static final String ACCESS_DENIED_URL = "/access-denied";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/items/*", "/images/**",
                                LOGIN_PAGE, ACCESS_DENIED_URL).permitAll()
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .anonymous(Customizer.withDefaults())
                .formLogin(form -> form.loginPage(LOGIN_PAGE))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutHandler(new DelegatingServerLogoutHandler(
                                new SecurityContextServerLogoutHandler(),
                                new WebSessionServerLogoutHandler(),
                                new CookieClearingServerLogoutHandler(),
                                new HeaderWriterServerLogoutHandler(
                                        new ClearSiteDataServerHttpHeadersWriter(COOKIES, STORAGE, CACHE))))
                        .logoutSuccessHandler(logoutSuccessHandler()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint(LOGIN_REQUIRED_URL))
                        .accessDeniedHandler(accessDeniedHandler()))
                .csrf(csrf -> csrf.csrfTokenRequestHandler(new MultipartAwareCsrfTokenRequestHandler()))
                .build();
    }

    private static RedirectServerLogoutSuccessHandler logoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler handler = new RedirectServerLogoutSuccessHandler();
        handler.setLogoutSuccessUrl(URI.create(LOGOUT_SUCCESS_URL));
        return handler;
    }

    private static ServerAccessDeniedHandler accessDeniedHandler() {
        DefaultServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();
        return (exchange, denied) -> redirectStrategy.sendRedirect(exchange, URI.create(ACCESS_DENIED_URL));
    }
}
