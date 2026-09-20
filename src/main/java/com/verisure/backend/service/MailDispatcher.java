package com.verisure.backend.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Envío real del correo, fuera del hilo de la petición.
 *
 * <p>Está en un bean aparte a propósito. {@code @Async} vive en un
 * <i>proxy</i> que Spring pone alrededor del bean, igual que
 * {@code @Transactional}. Si este método estuviera en
 * {@link NotificationServiceImpl} y se llamara desde otro método de esa misma
 * clase, la llamada no pasaría por el proxy y la anotación se ignoraría sin
 * dar ningún error: el correo volvería a enviarse en el hilo de la
 * petición. Separarlo es lo que garantiza que el proxy se aplica.
 *
 * <p>El {@code try/catch} está aquí dentro porque una excepción en un método
 * asíncrono no llega a quien lo llamó: sin capturarla no quedaría ni rastro de
 * que los correos no salen.
 *
 * <p>Dueña: BE3 · Tarea: B3-09.
 */
@Slf4j
@Component
public class MailDispatcher {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String from;

    public MailDispatcher(JavaMailSender mailSender,
                          TemplateEngine templateEngine,
                          @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from;
    }

    @Async
    public void dispatch(MailMessage message) {
        try {
            mailSender.send(build(message));
            log.info("Correo «{}» enviado a {}", message.subject(), message.recipient());
        } catch (Exception e) {
            logFailure(message, e);
        }
    }

    private MimeMessage build(MailMessage message) throws MessagingException {
        Context context = new Context();
        context.setVariables(message.variables());
        String body = templateEngine.process(message.template(), context);

        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(message.recipient());
        helper.setSubject(message.subject());
        helper.setText(body, true);
        return mime;
    }

    /**
     * El fallo del correo de verificación sube a error porque deja a una
     * entidad bloqueada sin saber por qué; el resto son avisos.
     */
    private void logFailure(MailMessage message, Exception e) {
        if (message.critical()) {
            log.error("No se pudo enviar «{}» a {}: {}",
                    message.subject(), message.recipient(), e.getMessage());
        } else {
            log.warn("No se pudo enviar «{}» a {}: {}",
                    message.subject(), message.recipient(), e.getMessage());
        }
    }
}
