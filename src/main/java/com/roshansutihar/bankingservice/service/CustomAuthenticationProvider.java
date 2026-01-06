package com.roshansutihar.bankingservice.service;


import com.roshansutihar.bankingservice.entity.User;
import com.roshansutihar.bankingservice.enums.UserStatus;
import com.roshansutihar.bankingservice.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    //these codes are AI generated I used ChatGPT for this
    private final AuthService authService;
    private final UserRepository userRepository;

    public CustomAuthenticationProvider(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Account is not active");
        }

        if (authService.authenticateUser(user.getId(), password)) {

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            String typeRole = switch (user.getUserType()) {
                case INDIVIDUAL -> "ROLE_INDIVIDUAL";
                case BUSINESS -> "ROLE_BUSINESS";
                default -> "ROLE_USER";
            };
            authorities.add(new SimpleGrantedAuthority(typeRole));

            return new UsernamePasswordAuthenticationToken(username, password, authorities);
        } else {
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
