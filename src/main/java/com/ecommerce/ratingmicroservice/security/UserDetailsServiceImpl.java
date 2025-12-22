package com.ecommerce.ratingmicroservice.security;

import com.ecommerce.ratingmicroservice.entity.User;
import com.ecommerce.ratingmicroservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail()) // Spring Security uses username field → we use email as login ID
                .password(user.getPassword())
                .authorities(getAuthorities(user.getRoles()))
//                .disabled(!user.isEmailVerified())
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
    }

}