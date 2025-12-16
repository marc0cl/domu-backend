package com.domu.email;

import com.domu.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.Properties;

public class SmtpEmailService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailService.class);

    private final AppConfig config;
    private final Session session;
    private final boolean enabled;

    public SmtpEmailService(AppConfig config) {
        this.config = config;
        this.enabled = config.mailHost() != null && !config.mailHost().isBlank();
        if (!enabled) {
            this.session = null;
            LOGGER.warn("SMTP no configurado (MAIL_HOST vacío). Se omitirán envíos de correo.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", config.mailHost());
        props.put("mail.smtp.port", config.mailPort() != null ? config.mailPort() : 587);
        props.put("mail.smtp.auth", config.mailUser() != null && !config.mailUser().isBlank());
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", config.mailHost());

        Authenticator authenticator = null;
        if (config.mailUser() != null && !config.mailUser().isBlank()) {
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.mailUser(), config.mailPassword());
                }
            };
        }
        this.session = Session.getInstance(props, authenticator);
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        if (!enabled) {
            LOGGER.info("SMTP deshabilitado. Correo a {} con asunto '{}' no será enviado.", to, subject);
            return;
        }
        try {
            MimeMessage message = buildBaseMessage(to, subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(message);
            LOGGER.info("Correo enviado a {}", to);
        } catch (MessagingException e) {
            LOGGER.error("Error enviando correo a {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendHtmlWithAttachment(String to, String subject, String htmlBody, String attachmentName, String attachmentContentType, byte[] attachmentContent) {
        if (!enabled) {
            LOGGER.info("SMTP deshabilitado. Correo con adjunto a {} con asunto '{}' no será enviado.", to, subject);
            return;
        }
        try {
            MimeMessage message = buildBaseMessage(to, subject);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            String safeName = (attachmentName != null && !attachmentName.isBlank()) ? attachmentName : "documento.pdf";
            String safeContentType = (attachmentContentType != null && !attachmentContentType.isBlank())
                    ? attachmentContentType
                    : "application/octet-stream";
            attachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(attachmentContent, safeContentType)));
            attachmentPart.setFileName(safeName);

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
            Transport.send(message);
            LOGGER.info("Correo con adjunto enviado a {}", to);
        } catch (MessagingException e) {
            LOGGER.error("Error enviando correo con adjunto a {}: {}", to, e.getMessage());
        }
    }

    private MimeMessage buildBaseMessage(String to, String subject) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.mailFrom()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");
        return message;
    }
}

