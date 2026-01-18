package com.confiance.notification.service.otp;

import com.confiance.notification.dto.OtpResponse;
import com.confiance.notification.enums.OtpProvider;
import com.confiance.notification.enums.OtpPurpose;

public interface OtpSender {

    OtpResponse sendOtp(String identifier, OtpPurpose purpose, String countryCode);

    OtpResponse verifyOtp(String identifier, String otp, OtpPurpose purpose);

    OtpProvider getProvider();

    boolean isConfigured();
}
