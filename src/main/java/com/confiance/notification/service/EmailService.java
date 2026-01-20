package com.confiance.notification.service;

import com.confiance.notification.dto.EmailRequest;
import com.confiance.notification.dto.EmailResponse;
import com.confiance.notification.entity.EmailLog;
import com.confiance.notification.entity.EmailTemplate;
import com.confiance.notification.enums.EmailProvider;
import com.confiance.notification.repository.EmailLogRepository;
import com.confiance.notification.repository.EmailTemplateRepository;
import com.confiance.notification.service.email.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmailService {

    private final Map<EmailProvider, EmailSender> emailSenders;
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final TemplateEngine templateEngine;
    private final FeatureService featureService;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Value("${email.provider:smtp}")
    private String defaultProvider;

    @Autowired
    public EmailService(List<EmailSender> senders, EmailLogRepository emailLogRepository,
                        EmailTemplateRepository emailTemplateRepository, TemplateEngine templateEngine,
                        FeatureService featureService) {
        this.emailSenders = senders.stream()
                .collect(Collectors.toMap(EmailSender::getProvider, sender -> sender));
        this.emailLogRepository = emailLogRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.templateEngine = templateEngine;
        this.featureService = featureService;
        log.info("Email service initialized with providers: {}", emailSenders.keySet());
    }

    public EmailResponse sendEmail(EmailRequest request) {
        return sendEmail(request, null);
    }

    public EmailResponse sendEmail(EmailRequest request, Long userId) {
        EmailProvider provider = EmailProvider.valueOf(defaultProvider.toUpperCase().replace("-", "_"));
        return sendEmailWithProvider(request, provider, userId);
    }

    public EmailResponse sendEmailWithProvider(EmailRequest request, EmailProvider provider, Long userId) {
        // Check if EMAIL feature is enabled globally
        if (!featureService.isEnabled(FeatureService.FEATURE_EMAIL)) {
            log.warn("Email feature is DISABLED - skipping email to: {}", request.getTo());
            EmailResponse skippedResponse = EmailResponse.builder()
                    .status("SKIPPED")
                    .recipient(request.getTo())
                    .sentAt(LocalDateTime.now())
                    .build();
            saveEmailLog(request, skippedResponse, provider, userId);
            return skippedResponse;
        }

        // Check specific template-based feature flags
        String templateName = request.getTemplateName();
        if (templateName != null) {
            String featureCode = getFeatureCodeForTemplate(templateName);
            if (featureCode != null && !featureService.isEnabled(featureCode)) {
                log.warn("Feature {} is DISABLED - skipping {} email to: {}", featureCode, templateName, request.getTo());
                EmailResponse skippedResponse = EmailResponse.builder()
                        .status("SKIPPED")
                        .recipient(request.getTo())
                        .sentAt(LocalDateTime.now())
                        .build();
                saveEmailLog(request, skippedResponse, provider, userId);
                return skippedResponse;
            }
        }

        EmailSender sender = emailSenders.get(provider);

        if (sender == null || !sender.isConfigured()) {
            log.warn("Email provider {} not available or not configured, falling back to SMTP", provider);
            sender = emailSenders.get(EmailProvider.SMTP);
        }

        if (sender == null) {
            log.error("No email sender available");
            return EmailResponse.builder()
                    .status("FAILED")
                    .recipient(request.getTo())
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        if (request.getTemplateName() != null && request.getTemplateVariables() != null) {
            String processedBody = processTemplate(request.getTemplateName(), request.getTemplateVariables());
            request.setBody(processedBody);
            request.setHtml(true);
        }

        EmailResponse response = sender.send(request);

        saveEmailLog(request, response, provider, userId);

        return response;
    }

    /**
     * Map template names to feature codes for granular control
     */
    private String getFeatureCodeForTemplate(String templateName) {
        return switch (templateName.toLowerCase()) {
            case "welcome" -> FeatureService.FEATURE_WELCOME_EMAIL;
            case "password-reset" -> FeatureService.FEATURE_PASSWORD_RESET;
            case "payment-success" -> FeatureService.FEATURE_PAYMENT_NOTIFICATION;
            default -> null; // No specific feature flag, use global EMAIL flag
        };
    }

    @Async
    public void sendEmailAsync(EmailRequest request) {
        sendEmailAsync(request, null);
    }

    @Async
    public void sendEmailAsync(EmailRequest request, Long userId) {
        sendEmail(request, userId);
    }

    public EmailResponse sendTemplatedEmail(String to, String subject, String templateName,
                                            Map<String, Object> variables, Long userId) {
        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject(subject)
                .templateName(templateName)
                .templateVariables(variables)
                .isHtml(true)
                .build();
        return sendEmail(request, userId);
    }

    public void sendSimpleEmail(String to, String subject, String body) {
        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject(subject)
                .body(body)
                .isHtml(false)
                .build();
        sendEmail(request);
    }

    private String processTemplate(String templateName, Map<String, Object> variables) {
        // First, try to get template from database
        Optional<EmailTemplate> dbTemplate = emailTemplateRepository.findByCodeAndIsActiveTrue(templateName);

        if (dbTemplate.isPresent()) {
            log.debug("Using DB template for: {}", templateName);
            return processDbTemplate(dbTemplate.get().getHtmlContent(), variables);
        }

        // Fallback to Thymeleaf filesystem template
        try {
            log.debug("Using Thymeleaf template for: {}", templateName);
            Context context = new Context();
            context.setVariables(variables);
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("Error processing email template {}: {}", templateName, e.getMessage());
            return "";
        }
    }

    private String processDbTemplate(String templateContent, Map<String, Object> variables) {
        if (templateContent == null || variables == null || variables.isEmpty()) {
            return templateContent;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private void saveEmailLog(EmailRequest request, EmailResponse response, EmailProvider provider, Long userId) {
        try {
            EmailLog log = EmailLog.builder()
                    .recipient(request.getTo())
                    .cc(request.getCc() != null ? String.join(",", request.getCc()) : null)
                    .bcc(request.getBcc() != null ? String.join(",", request.getBcc()) : null)
                    .subject(request.getSubject())
                    .body(request.getBody())
                    .templateName(request.getTemplateName())
                    .status(response.getStatus())
                    .messageId(response.getMessageId())
                    .provider(provider)
                    .userId(userId)
                    .sentAt("SENT".equals(response.getStatus()) ? response.getSentAt() : null)
                    .build();
            emailLogRepository.save(log);
        } catch (Exception e) {
            EmailService.log.error("Error saving email log: {}", e.getMessage());
        }
    }

    public Optional<EmailSender> getEmailSender(EmailProvider provider) {
        return Optional.ofNullable(emailSenders.get(provider));
    }

    public boolean isProviderConfigured(EmailProvider provider) {
        EmailSender sender = emailSenders.get(provider);
        return sender != null && sender.isConfigured();
    }
}
