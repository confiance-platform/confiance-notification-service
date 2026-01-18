package com.confiance.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    private List<String> cc;

    private List<String> bcc;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String body;

    private String templateName;

    private Map<String, Object> templateVariables;

    private boolean isHtml;

    private List<EmailAttachment> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailAttachment {
        private String fileName;
        private String contentType;
        private byte[] content;
        private String url;
    }
}
