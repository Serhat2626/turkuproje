package com.anadoluatlasi.backend.auth.service;

import com.anadoluatlasi.backend.auth.entity.AdminUser;
import com.anadoluatlasi.backend.auth.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security UserDetailsService implementasyonu.
 * AdminUser entity'sinden UserDetails oluşturur.
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Kullanıcı bulunamadı: " + username));

        return new User(
                adminUser.getUsername(),
                adminUser.getPasswordHash(),
                adminUser.isEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                !adminUser.isAccountLocked(),
                List.of(new SimpleGrantedAuthority(adminUser.getRole().toAuthority()))
        );
    }
}
