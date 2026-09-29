package ru.yandex.practicum.mymarket.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MarketUserDetailsService userDetailsService;

    @Test
    void findByUsername_returnsMarketUserWithIdPasswordHashAndRole() {
        User user = new User("admin", "$2a$10$hash", Role.ADMIN);
        user.setId(3L);
        when(userRepository.findByUsername("admin")).thenReturn(Mono.just(user));

        StepVerifier.create(userDetailsService.findByUsername("admin"))
                .assertNext(details -> {
                    assertThat(details).isInstanceOf(MarketUser.class);
                    assertThat(((MarketUser) details).getId()).isEqualTo(3L);
                    assertThat(details.getUsername()).isEqualTo("admin");
                    assertThat(details.getPassword()).isEqualTo("$2a$10$hash");
                    assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                            .containsExactly("ROLE_ADMIN");
                })
                .verifyComplete();
    }

    @Test
    void findByUsername_unknownUser_returnsEmpty() {
        when(userRepository.findByUsername("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(userDetailsService.findByUsername("ghost")).verifyComplete();
    }
}
