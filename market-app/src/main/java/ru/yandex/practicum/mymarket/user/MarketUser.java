package ru.yandex.practicum.mymarket.user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Objects;

public class MarketUser extends org.springframework.security.core.userdetails.User {

    private final long id;

    public MarketUser(long id, String username, String password, Role role) {
        super(username, password, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        this.id = id;
    }

    public static MarketUser of(User user) {
        return new MarketUser(user.getId(), user.getUsername(), user.getPassword(), user.getRole());
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MarketUser user && super.equals(user) && id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
