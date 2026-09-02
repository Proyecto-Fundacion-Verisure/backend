package com.verisure.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Envío real del correo, fuera del hilo de la petición.
 *
 * <p><b>Está en un bean aparte a propósito.</b> {@code @Async} vive en un
 * <i>proxy</i> que Spring pone alrededor del bean, igual que
 * {@code @Transactional}. Si este método estuviera en
 * {@link NotificationServiceImpl} y se llamara desde otro método de esa misma
 * clase, la llamada no pasaría por el proxy y <b>la anotación se ignoraría sin
 * dar ningún error</b>: el correo volvería a enviarse en el hilo de la
 * petición. Separarlo es lo que garantiza que el proxy se aplica.
 *
 * <p>El {@code try/catch} está aquí dentro porque una excepción en un método
 * asíncrono no llega a quien lo llamó: sin capturarla no quedaría ni rastro de
 * que los correos no salen.
 */
@Slf4j
@Component
public class MailDispatcher {

    @Async
    public void dispatch(String subject, String kind, Long id) {
        try {
            // TODO B3-09 · sustituir por mailService.send(...) con su plantilla,
            // construyendo el enlace con app.base-url + la ruta que entrega FE2.
            log.info("TODO B3-09 · correo «{}» para {} {}", subject, kind, id);
        } catch (Exception e) {
            log.warn("No se pudo enviar «{}» para {} {}: {}", subject, kind, id, e.getMessage());
        }
    }
}
