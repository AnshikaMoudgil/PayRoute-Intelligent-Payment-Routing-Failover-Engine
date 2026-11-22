package com.payroute.security;

import com.payroute.entity.Merchant;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class MerchantPrincipal implements UserDetails {

    private final Merchant merchant;

    public MerchantPrincipal(Merchant merchant) {
        this.merchant = merchant;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MERCHANT"));
    }

    @Override
    public String getPassword() {
        return merchant.getApiKeyHash();
    }

    @Override
    public String getUsername() {
        return merchant.getMerchantCode();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return merchant.getStatus() == com.payroute.entity.ProviderStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return merchant.getStatus() == com.payroute.entity.ProviderStatus.ACTIVE;
    }
}
