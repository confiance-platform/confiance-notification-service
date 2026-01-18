package com.confiance.notification.service.email;

import com.confiance.notification.dto.EmailRequest;
import com.confiance.notification.dto.EmailResponse;
import com.confiance.notification.enums.EmailProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${email.from.address:noreply@confiance.com}")
    private String fromAddress;

    @Value("${email.from.name:Confiance Financial}")
    private String fromName;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Override
    public EmailResponse send(EmailRequest request) {
        String messageId = UUID.randomUUID().toString();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            if (request.isHtml()) {
                helper.setText(request.getBody(), true);
            } else {
                helper.setText(request.getBody());
            }

            if (request.getAttachments() != null) {
                for (EmailRequest.EmailAttachment attachment : request.getAttachments()) {
                    if (attachment.getContent() != null) {
                        helper.addAttachment(attachment.getFileName(),
                                new ByteArrayResource(attachment.getContent()),
                                attachment.getContentType());
                    }
                }
            }

            mailSender.send(message);

            log.info("Email sent successfully via SMTP to: {}", request.getTo());

            return EmailResponse.builder()
                    .messageId(messageId)
                    .status("SENT")
                    .recipient(request.getTo())
                    .sentAt(LocalDateTime.now())
                    .provider(getProvider().name())
                    .build();

        } catch (MessagingException e) {
            log.error("Failed to send email via SMTP: {}", e.getMessage());
            return EmailResponse.builder()
                    .messageId(messageId)
                    .status("FAILED")
                    .recipient(request.getTo())
                    .sentAt(LocalDateTime.now())
                    .provider(getProvider().name())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error sending email via SMTP: {}", e.getMessage());
            return EmailResponse.builder()
                    .messageId(messageId)
                    .status("FAILED")
                    .recipient(request.getTo())
                    .sentAt(LocalDateTime.now())
                    .provider(getProvider().name())
                    .build();
        }
    }

    @Override
    public EmailProvider getProvider() {
        return EmailProvider.SMTP;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(mailUsername);
    }
}
