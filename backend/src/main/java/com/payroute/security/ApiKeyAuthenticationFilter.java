package com.payroute.security;

import com.payroute.entity.Merchant;
import com.payroute.entity.ProviderStatus;
import com.payroute.repository.MerchantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final MerchantRepository merchantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = extractApiKey(request);

        if (StringUtils.hasText(apiKey)) {
            String hash = hashSha256(apiKey);
            Optional<Merchant> merchantOpt = merchantRepository.findByApiKeyHash(hash);

            if (merchantOpt.isPresent()) {
                Merchant merchant = merchantOpt.get();
                if (merchant.getStatus() == ProviderStatus.ACTIVE) {
                    MerchantPrincipal principal = new MerchantPrincipal(merchant);
                    ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
                            principal,
                            apiKey,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_MERCHANT"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.warn("Attempt to authenticate with suspended merchant: {}", merchant.getMerchantCode());
                }
            } else if ("pr_admin_master_key_999".equals(apiKey)) {
                // Admin Master Key for Operator / Dev Dashboard
                ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
                        "admin",
                        apiKey,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } else {
            // Check if X-Merchant-Code header is passed (convenience for API Playground / testing)
            String merchantCode = request.getHeader("X-Merchant-Code");
            if (StringUtils.hasText(merchantCode)) {
                merchantRepository.findByMerchantCode(merchantCode).ifPresent(merchant -> {
                    if (merchant.getStatus() == ProviderStatus.ACTIVE) {
                        MerchantPrincipal principal = new MerchantPrincipal(merchant);
                        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
                                principal,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_MERCHANT"))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }

        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        return null;
    }

    public static String hashSha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
