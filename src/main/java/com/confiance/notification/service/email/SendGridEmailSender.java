package com.confiance.notification.service.email;

import com.confiance.notification.dto.EmailRequest;
import com.confiance.notification.dto.EmailResponse;
import com.confiance.notification.enums.EmailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "email.provider", havingValue = "sendgrid")
public class SendGridEmailSender implements EmailSender {

    @Value("${sendgrid.api-key:}")
    private String apiKey;

    @Value("${email.from.address:noreply@confiance.com}")
    private String fromAddress;

    @Value("${email.from.name:Confiance Financial}")
    private String fromName;

    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

    @Override
    public EmailResponse send(EmailRequest request) {
        String messageId = UUID.randomUUID().toString();

        try {
            String jsonPayload = buildJsonPayload(request);

            URL url = new URL(SENDGRID_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                log.info("Email sent successfully via SendGrid to: {}", request.getTo());
                return EmailResponse.builder()
                        .messageId(messageId)
                        .status("SENT")
                        .recipient(request.getTo())
                        .sentAt(LocalDateTime.now())
                        .provider(getProvider().name())
                        .build();
            } else {
                log.error("Failed to send email via SendGrid. Response code: {}", responseCode);
                return EmailResponse.builder()
                        .messageId(messageId)
                        .status("FAILED")
                        .recipient(request.getTo())
                        .sentAt(LocalDateTime.now())
                        .provider(getProvider().name())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error sending email via SendGrid: {}", e.getMessage());
            return EmailResponse.builder()
                    .messageId(messageId)
                    .status("FAILED")
                    .recipient(request.getTo())
                    .sentAt(LocalDateTime.now())
                    .provider(getProvider().name())
                    .build();
        }
    }

    private String buildJsonPayload(EmailRequest request) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"personalizations\": [{\"to\": [{\"email\": \"").append(escapeJson(request.getTo())).append("\"}]");

        if (request.getCc() != null && !request.getCc().isEmpty()) {
            json.append(",\"cc\": [");
            for (int i = 0; i < request.getCc().size(); i++) {
                if (i > 0) json.append(",");
                json.append("{\"email\": \"").append(escapeJson(request.getCc().get(i))).append("\"}");
            }
            json.append("]");
        }

        json.append("}],");
        json.append("\"from\": {\"email\": \"").append(escapeJson(fromAddress)).append("\", \"name\": \"").append(escapeJson(fromName)).append("\"},");
        json.append("\"subject\": \"").append(escapeJson(request.getSubject())).append("\",");
        json.append("\"content\": [{\"type\": \"").append(request.isHtml() ? "text/html" : "text/plain").append("\", \"value\": \"").append(escapeJson(request.getBody())).append("\"}]");

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            json.append(",\"attachments\": [");
            for (int i = 0; i < request.getAttachments().size(); i++) {
                EmailRequest.EmailAttachment att = request.getAttachments().get(i);
                if (i > 0) json.append(",");
                json.append("{\"content\": \"").append(Base64.getEncoder().encodeToString(att.getContent())).append("\",");
                json.append("\"filename\": \"").append(escapeJson(att.getFileName())).append("\",");
                json.append("\"type\": \"").append(escapeJson(att.getContentType())).append("\"}");
            }
            json.append("]");
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public EmailProvider getProvider() {
        return EmailProvider.SENDGRID;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }
}
