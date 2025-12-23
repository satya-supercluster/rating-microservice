package com.ecommerce.ratingmicroservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String email;
    private final String password;
    private final boolean emailVerified;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public String getUsername() {
        return email; // email used as login identifier
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Matches `.disabled(!user.isEmailVerified())`
        return emailVerified;
    }

    public boolean hasRole(String role) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.toUpperCase()));
    }
}
