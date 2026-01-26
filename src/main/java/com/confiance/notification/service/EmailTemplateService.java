package com.confiance.notification.service;

import com.confiance.notification.entity.EmailTemplate;
import com.confiance.notification.repository.EmailTemplateRepository;
import com.confiance.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public Optional<EmailTemplate> findByCode(String code) {
        return templateRepository.findByCodeAndIsActiveTrue(code);
    }

    public List<EmailTemplate> getAllActiveTemplates() {
        return templateRepository.findByIsActiveTrue();
    }

    public List<EmailTemplate> getTemplatesByCategory(String category) {
        return templateRepository.findByCategoryAndIsActiveTrue(category);
    }

    @Transactional
    public EmailTemplate createTemplate(EmailTemplate template) {
        if (templateRepository.existsByCode(template.getCode())) {
            throw new IllegalArgumentException("Template with code '" + template.getCode() + "' already exists");
        }
        return templateRepository.save(template);
    }

    @Transactional
    public EmailTemplate updateTemplate(String code, EmailTemplate updatedTemplate) {
        EmailTemplate existing = templateRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with code: " + code));

        existing.setName(updatedTemplate.getName());
        existing.setSubject(updatedTemplate.getSubject());
        existing.setHtmlContent(updatedTemplate.getHtmlContent());
        existing.setPlainTextContent(updatedTemplate.getPlainTextContent());
        existing.setDescription(updatedTemplate.getDescription());
        existing.setAvailableVariables(updatedTemplate.getAvailableVariables());
        existing.setCategory(updatedTemplate.getCategory());
        existing.setIsActive(updatedTemplate.getIsActive());

        return templateRepository.save(existing);
    }

    @Transactional
    public void deleteTemplate(String code) {
        EmailTemplate template = templateRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with code: " + code));
        template.setIsActive(false);
        templateRepository.save(template);
    }

    public String processTemplate(String templateContent, Map<String, Object> variables) {
        if (templateContent == null || variables == null || variables.isEmpty()) {
            return templateContent;
        }

        String processed = templateContent;
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

    public ProcessedTemplate processEmailTemplate(String templateCode, Map<String, Object> variables) {
        EmailTemplate template = findByCode(templateCode)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + templateCode));

        String processedSubject = processTemplate(template.getSubject(), variables);
        String processedHtml = processTemplate(template.getHtmlContent(), variables);
        String processedPlainText = template.getPlainTextContent() != null
                ? processTemplate(template.getPlainTextContent(), variables)
                : null;

        return ProcessedTemplate.builder()
                .subject(processedSubject)
                .htmlContent(processedHtml)
                .plainTextContent(processedPlainText)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProcessedTemplate {
        private String subject;
        private String htmlContent;
        private String plainTextContent;
    }
}
