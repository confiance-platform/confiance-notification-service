package com.confiance.notification.config;

import com.confiance.notification.entity.EmailTemplate;
import com.confiance.notification.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateInitializer implements CommandLineRunner {

    private final EmailTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        initializeTemplates();
    }

    private void initializeTemplates() {
        createPasswordResetTemplate();
        createWelcomeTemplate();
        createOtpTemplate();
        createPaymentSuccessTemplate();
        log.info("Email templates initialization completed");
    }

    private void createPasswordResetTemplate() {
        if (templateRepository.existsByCode("password-reset")) {
            log.debug("Password reset template already exists");
            return;
        }

        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">Password Reset Request</h1>
                                    </td>
                                </tr>

                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px;">
                                        <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #333333;">
                                            Hello <strong>${firstName}</strong>,
                                        </p>
                                        <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #333333;">
                                            We received a request to reset your password for your Confiance account. Click the button below to reset it.
                                        </p>

                                        <!-- Button -->
                                        <table role="presentation" style="width: 100%; border-collapse: collapse;">
                                            <tr>
                                                <td align="center" style="padding: 20px 0;">
                                                    <a href="${resetLink}" style="display: inline-block; padding: 14px 40px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">
                                                        Reset Password
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>

                                        <p style="margin: 20px 0; font-size: 14px; line-height: 1.6; color: #666666;">
                                            This link will expire in <strong>${expiryHours} hours</strong>.
                                        </p>

                                        <p style="margin: 20px 0 0 0; font-size: 14px; line-height: 1.6; color: #666666;">
                                            If you didn't request a password reset, please ignore this email or contact support if you have concerns.
                                        </p>

                                        <!-- Divider -->
                                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #eeeeee;">

                                        <p style="margin: 0; font-size: 12px; color: #999999;">
                                            If the button doesn't work, copy and paste this link into your browser:
                                        </p>
                                        <p style="margin: 10px 0 0 0; font-size: 12px; color: #667eea; word-break: break-all;">
                                            ${resetLink}
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 20px 40px; text-align: center; background-color: #f8f9fa; border-radius: 0 0 8px 8px;">
                                        <p style="margin: 0; font-size: 12px; color: #999999;">
                                            &copy; 2024 Confiance. All rights reserved.
                                        </p>
                                        <p style="margin: 10px 0 0 0; font-size: 12px; color: #999999;">
                                            This is an automated message, please do not reply.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

        String plainText = """
            Password Reset Request

            Hello ${firstName},

            We received a request to reset your password for your Confiance account.

            Click the link below to reset your password:
            ${resetLink}

            This link will expire in ${expiryHours} hours.

            If you didn't request a password reset, please ignore this email.

            © 2024 Confiance. All rights reserved.
            """;

        EmailTemplate template = EmailTemplate.builder()
                .code("password-reset")
                .name("Password Reset Email")
                .subject("Password Reset Request - Confiance")
                .htmlContent(htmlContent)
                .plainTextContent(plainText)
                .description("Email sent when user requests password reset")
                .availableVariables("firstName, resetLink, resetToken, expiryHours")
                .category("TRANSACTIONAL")
                .isActive(true)
                .build();

        templateRepository.save(template);
        log.info("Created password-reset email template");
    }

    private void createWelcomeTemplate() {
        if (templateRepository.existsByCode("welcome")) {
            log.debug("Welcome template already exists");
            return;
        }

        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to Confiance</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">Welcome to Confiance!</h1>
                                    </td>
                                </tr>

                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px;">
                                        <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #333333;">
                                            Hello <strong>${firstName} ${lastName}</strong>,
                                        </p>
                                        <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #333333;">
                                            Welcome to Confiance! Your account has been successfully created.
                                        </p>

                                        <!-- Account Details -->
                                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0;">
                                            <h3 style="margin: 0 0 15px 0; color: #333333;">Your Account Details:</h3>
                                            <p style="margin: 5px 0; font-size: 14px; color: #666666;">
                                                <strong>Email:</strong> ${email}
                                            </p>
                                            <p style="margin: 5px 0; font-size: 14px; color: #666666;">
                                                <strong>Referral Code:</strong> ${referralCode}
                                            </p>
                                        </div>

                                        <!-- Button -->
                                        <table role="presentation" style="width: 100%; border-collapse: collapse;">
                                            <tr>
                                                <td align="center" style="padding: 20px 0;">
                                                    <a href="${loginUrl}" style="display: inline-block; padding: 14px 40px; background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">
                                                        Login to Your Account
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 20px 40px; text-align: center; background-color: #f8f9fa; border-radius: 0 0 8px 8px;">
                                        <p style="margin: 0; font-size: 12px; color: #999999;">
                                            &copy; 2024 Confiance. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

        EmailTemplate template = EmailTemplate.builder()
                .code("welcome")
                .name("Welcome Email")
                .subject("Welcome to Confiance - ${firstName}!")
                .htmlContent(htmlContent)
                .description("Email sent to new users upon registration")
                .availableVariables("firstName, lastName, email, referralCode, loginUrl")
                .category("TRANSACTIONAL")
                .isActive(true)
                .build();

        templateRepository.save(template);
        log.info("Created welcome email template");
    }

    private void createOtpTemplate() {
        if (templateRepository.existsByCode("otp")) {
            log.debug("OTP template already exists");
            return;
        }

        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Your OTP Code</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">Verification Code</h1>
                                    </td>
                                </tr>

                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px; text-align: center;">
                                        <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #333333;">
                                            Your verification code for <strong>${purpose}</strong> is:
                                        </p>

                                        <!-- OTP Code -->
                                        <div style="background-color: #f8f9fa; padding: 30px; border-radius: 8px; margin: 20px 0;">
                                            <span style="font-size: 42px; font-weight: 700; letter-spacing: 8px; color: #333333;">${otp}</span>
                                        </div>

                                        <p style="margin: 20px 0; font-size: 14px; line-height: 1.6; color: #666666;">
                                            This code will expire in <strong>${expiryMinutes} minutes</strong>.
                                        </p>

                                        <p style="margin: 20px 0 0 0; font-size: 14px; line-height: 1.6; color: #999999;">
                                            If you didn't request this code, please ignore this email.
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 20px 40px; text-align: center; background-color: #f8f9fa; border-radius: 0 0 8px 8px;">
                                        <p style="margin: 0; font-size: 12px; color: #999999;">
                                            &copy; 2024 Confiance. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

        EmailTemplate template = EmailTemplate.builder()
                .code("otp")
                .name("OTP Verification Email")
                .subject("Your Verification Code - ${otp}")
                .htmlContent(htmlContent)
                .description("Email sent with OTP for verification")
                .availableVariables("otp, purpose, expiryMinutes")
                .category("TRANSACTIONAL")
                .isActive(true)
                .build();

        templateRepository.save(template);
        log.info("Created OTP email template");
    }

    private void createPaymentSuccessTemplate() {
        if (templateRepository.existsByCode("payment-success")) {
            log.debug("Payment success template already exists");
            return;
        }

        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Payment Successful</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" style="width: 100%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">Payment Successful!</h1>
                                    </td>
                                </tr>

                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px;">
                                        <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #333333;">
                                            Hello <strong>${customerName}</strong>,
                                        </p>
                                        <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #333333;">
                                            Your payment has been successfully processed. Here are the details:
                                        </p>

                                        <!-- Payment Details -->
                                        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0;">
                                            <table style="width: 100%; border-collapse: collapse;">
                                                <tr>
                                                    <td style="padding: 8px 0; color: #666666;">Order ID:</td>
                                                    <td style="padding: 8px 0; color: #333333; text-align: right; font-weight: 600;">${orderId}</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 8px 0; color: #666666;">Amount:</td>
                                                    <td style="padding: 8px 0; color: #333333; text-align: right; font-weight: 600;">${currency} ${amount}</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 8px 0; color: #666666;">Payment ID:</td>
                                                    <td style="padding: 8px 0; color: #333333; text-align: right; font-weight: 600;">${paymentId}</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 8px 0; color: #666666;">Date:</td>
                                                    <td style="padding: 8px 0; color: #333333; text-align: right; font-weight: 600;">${paymentDate}</td>
                                                </tr>
                                            </table>
                                        </div>

                                        <p style="margin: 20px 0 0 0; font-size: 14px; line-height: 1.6; color: #666666;">
                                            Thank you for your payment. If you have any questions, please contact our support team.
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 20px 40px; text-align: center; background-color: #f8f9fa; border-radius: 0 0 8px 8px;">
                                        <p style="margin: 0; font-size: 12px; color: #999999;">
                                            &copy; 2024 Confiance. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

        EmailTemplate template = EmailTemplate.builder()
                .code("payment-success")
                .name("Payment Success Email")
                .subject("Payment Successful - Order #${orderId}")
                .htmlContent(htmlContent)
                .description("Email sent when payment is successful")
                .availableVariables("customerName, orderId, amount, currency, paymentId, paymentDate")
                .category("TRANSACTIONAL")
                .isActive(true)
                .build();

        templateRepository.save(template);
        log.info("Created payment-success email template");
    }
}
