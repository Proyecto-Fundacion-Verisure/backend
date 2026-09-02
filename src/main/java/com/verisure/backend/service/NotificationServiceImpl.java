package com.verisure.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;

/**
 * Implementación de los avisos.
 *
 * <p>Combina las dos garantías que hacen falta:
 * <ul>
 *   <li>El envío real lo hace {@link MailDispatcher}, que es un bean aparte con
 *       {@code @Async}, así que <b>nunca bloquea la respuesta</b>.</li>
 *   <li>Si hay una transacción abierta, el envío se difiere a
 *       {@code afterCommit()}, así que <b>el correo no sale si la operación se
 *       deshace</b> aunque alguien haya llamado al aviso desde dentro.</li>
 * </ul>
 *
 * <p>El orden importa: la sincronización se registra en el hilo del llamante,
 * donde la transacción existe, y solo el envío salta a otro hilo. Poner
 * {@code @Async} directamente en estos métodos rompería la primera garantía,
 * porque en el hilo asíncrono no hay ninguna transacción que sincronizar y el
 * correo saldría antes del commit.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final MailDispatcher mailDispatcher;

    @Override
    public void notifyRegistrationConfirmed(Long registrationId) {
        send("plaza confirmada", "inscripción", registrationId);
    }

    @Override
    public void notifyRegistrationWaitlisted(Long registrationId) {
        send("en lista de espera", "inscripción", registrationId);
    }

    @Override
    public void notifyRegistrationRejected(Long registrationId) {
        send("solicitud rechazada", "inscripción", registrationId);
    }

    @Override
    public void notifySpotReleased(Long registrationId) {
        send("plaza liberada · has ascendido", "inscripción", registrationId);
    }

    @Override
    public void notifyActivityCancelled(Long activityId) {
        send("actividad cancelada", "actividad", activityId);
    }

    @Override
    public void notifyActivityFinished(Long activityId) {
        send("actividad finalizada · cuéntanos cómo fue", "actividad", activityId);
    }

    @Override
    public void notifyActivityClosed(Long activityId) {
        send("actividad cerrada · certificado disponible", "actividad", activityId);
    }

    @Override
    public void notifyActivitySubmittedForReview(Long activityId) {
        send("actividad enviada a revisión", "actividad", activityId);
    }

    @Override
    public void notifyActivityApproved(Long activityId) {
        send("actividad aprobada", "actividad", activityId);
    }

    @Override
    public void notifyActivityReturned(Long activityId) {
        send("actividad devuelta para revisión", "actividad", activityId);
    }

    @Override
    public void notifyOrgAccountApproved(Long userId) {
        send("cuenta de entidad aprobada", "usuario", userId);
    }

    @Override
    public void notifyOrgAccountRejected(Long userId) {
        send("cuenta de entidad rechazada", "usuario", userId);
    }

    @Override
    public void notifyVerificationRequested(Long userId) {
        send("verifica tu correo", "usuario", userId);
    }

    /**
     * Difiere el envío al commit si hay transacción abierta; lo despacha en el
     * acto si no la hay, que es el caso de los tests y las tareas programadas.
     */
    private void send(String subject, String kind, Long id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mailDispatcher.dispatch(subject, kind, id);
                }
            });
        } else {
            mailDispatcher.dispatch(subject, kind, id);
        }
    }
}
