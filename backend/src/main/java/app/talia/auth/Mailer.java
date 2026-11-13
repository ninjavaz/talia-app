package app.talia.auth;

import app.talia.config.TaliaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends plain-text e-mails over SMTP (provider configured with spring.mail.*).
 * Asynchronous: the HTTP answer never waits for the mail server, so response times do not reveal
 * whether an e-mail was sent (and therefore whether an address has an account).
 */
@Component
public class Mailer {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    private final JavaMailSender sender;
    private final String from;

    Mailer(JavaMailSender sender, TaliaProperties properties) {
        this.sender = sender;
        this.from = properties.mail().from();
    }

    @Async
    public void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
        } catch (MailException e) {
            // the address is personal data: log only the domain
            log.warn("Could not send e-mail to *@{}: {}", to.substring(to.indexOf('@') + 1), e.getMessage());
        }
    }
}
