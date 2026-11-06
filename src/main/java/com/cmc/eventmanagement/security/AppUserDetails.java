package com.cmc.eventmanagement.security;

import com.cmc.eventmanagement.entity.RoleName;
import com.cmc.eventmanagement.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;

/** Principal có thêm id người dùng để Controller/Service biết "ai đang thao tác" (BR07, BR08). */
@Getter
public class AppUserDetails implements UserDetails {
    private final Long id;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public AppUserDetails(User u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.passwordHash = u.getPasswordHash();
        this.enabled = u.isEnabled();
        this.accountNonLocked = !u.isTemporarilyLocked(LocalDateTime.now());
        this.authorities = u.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName().name())).toList();
    }

    public boolean hasRole(RoleName role) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
    }

    public boolean isAdmin() { return hasRole(RoleName.ADMIN); }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
