package com.payroute.dto;

import com.payroute.entity.RoutingStrategyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantCreateRequest {

    @NotBlank(message = "Merchant code is required")
    @Size(min = 3, max = 50, message = "Merchant code must be between 3 and 50 characters")
    private String merchantCode;

    @NotBlank(message = "Merchant name is required")
    @Size(min = 2, max = 100, message = "Merchant name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Builder.Default
    private RoutingStrategyType defaultRoutingStrategy = RoutingStrategyType.PRIORITY;
}
