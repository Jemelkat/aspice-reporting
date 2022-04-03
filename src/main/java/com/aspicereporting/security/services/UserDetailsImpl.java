package com.aspicereporting.security.services;

import com.aspicereporting.entity.User;
import org.hibernate.Hibernate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserDetailsImpl extends User implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String email, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        super(id, username, email, password);
        this.authorities = authorities;
    }

    public UserDetailsImpl(User user,  Collection<? extends GrantedAuthority> authorities) {
        super(user.getId(), user.getUsername(),user.getEmail(),user.getPassword(),user.getRoles(),user.getUserGroups(),user.getSources(),user.getReports(),user.getTemplates(), user.getDashboard());
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        //Initialize lazy loaded usergroups to prevent issues with persistence bag
        Hibernate.initialize(user.getUserGroups());

        UserDetailsImpl u =  new UserDetailsImpl(user, authorities);
        return u;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(this.getId(), user.getId());
    }
}
