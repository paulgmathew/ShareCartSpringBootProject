package com.sharecart.sharecart.auth.service.impl;

import com.sharecart.sharecart.auth.service.VerificationEmailService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationEmailServiceImpl implements VerificationEmailService {

    private final RestClient restClient = RestClient.create();

    @Value("${app.mailtrap.api-token:}")
    private String mailtrapApiToken;

    @Value("${app.mailtrap.send-endpoint:https://send.api.mailtrap.io/api/send}")
    private String mailtrapSendEndpoint;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name:ShareCart}")
    private String fromName;

    @Value("${app.mailtrap.category:Account Verification}")
    private String category;

    @Value("classpath:templates/emails/verification-email.html")
    private Resource verificationEmailTemplate;

    @Override
    public void sendVerificationEmail(String toEmail, String recipientName, String verificationLink) {
        String displayName = (recipientName == null || recipientName.isBlank()) ? "there" : recipientName;
        if (mailtrapApiToken == null || mailtrapApiToken.isBlank()) {
            throw new IllegalStateException("Mailtrap API token is missing. Set MAILTRAP_API_TOKEN.");
        }

        String textBody =
                "Hi " + displayName + ",\n\n"
                        + "Thanks for signing up with ShareCart.\n"
                        + "Please verify your email using the link below:\n\n"
                        + verificationLink + "\n\n"
                        + "If you did not create this account, you can ignore this email.\n"
                        + "- ShareCart Team";
        String htmlBody = buildHtmlBody(displayName, verificationLink);

        Map<String, Object> payload = Map.of(
                "from", Map.of(
                        "email", fromAddress,
                        "name", fromName
                ),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "Verify your ShareCart account",
            "text", textBody,
            "html", htmlBody,
                "category", category
        );

        try {
            restClient.post()
                    .uri(mailtrapSendEndpoint)
                    .header("Authorization", "Bearer " + mailtrapApiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Verification email sent to={}", toEmail);
        } catch (RestClientException ex) {
            log.error("Failed to send verification email to={}", toEmail, ex);
            throw new IllegalStateException("Unable to send verification email. Please try again later.");
        }
    }

    private String buildHtmlBody(String displayName, String verificationLink) {
        String template = readTemplate();
        return template
                .replace("{{name}}", escapeHtml(displayName))
                .replace("{{verificationLink}}", escapeHtml(verificationLink));
    }

    private String readTemplate() {
        try (InputStream inputStream = verificationEmailTemplate.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load verification email template.", ex);
        }
    }

    private String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
