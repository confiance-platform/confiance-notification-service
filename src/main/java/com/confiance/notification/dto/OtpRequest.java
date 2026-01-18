package com.confiance.notification.dto;

import com.confiance.notification.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {

    @NotBlank(message = "Phone number or email is required")
    private String identifier;

    @NotNull(message = "OTP purpose is required")
    private OtpPurpose purpose;

    private Long userId;

    private String countryCode;
}
