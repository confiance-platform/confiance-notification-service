package com.confiance.notification.service.email;

import com.confiance.notification.dto.EmailRequest;
import com.confiance.notification.dto.EmailResponse;
import com.confiance.notification.enums.EmailProvider;

public interface EmailSender {

    EmailResponse send(EmailRequest request);

    EmailProvider getProvider();

    boolean isConfigured();
}
