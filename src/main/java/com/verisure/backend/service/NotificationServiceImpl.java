package com.verisure.backend.service;

import java.util.List;

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
 *       {@code @Async}, así que nunca bloquea la respuesta.</li>
 *   <li>Si hay una transacción abierta, el envío se difiere a
 *       {@code afterCommit()}, así que el correo no sale si la operación se
 *       deshace aunque alguien haya llamado al aviso desde dentro.</li>
 * </ul>
 *
 * <p>El orden importa: la sincronización se registra en el hilo del llamante,
 * donde la transacción existe, y solo el envío salta a otro hilo. Poner
 * {@code @Async} directamente en estos métodos rompería la primera garantía,
 * porque en el hilo asíncrono no hay ninguna transacción que sincronizar y el
 * correo saldría antes del commit.
 *
 * <p>El contenido se resuelve antes de diferir, y no dentro del
 * {@code afterCommit()}: con {@code spring.jpa.open-in-view=false} allí ya no
 * hay sesión con la que leer.
 *
 * <p>Dueña: BE3 · Tarea: B3-09.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final MailDispatcher mailDispatcher;
    private final MailContentFactory mailContentFactory;

    @Override
    public void notifyRegistrationConfirmed(Long registrationId) {
        send(mailContentFactory.registrationConfirmed(registrationId));
    }

    @Override
    public void notifyRegistrationWaitlisted(Long registrationId) {
        send(mailContentFactory.registrationWaitlisted(registrationId));
    }

    @Override
    public void notifyRegistrationRejected(Long registrationId) {
        send(mailContentFactory.registrationRejected(registrationId));
    }

    @Override
    public void notifySpotReleased(Long registrationId) {
        send(mailContentFactory.spotReleased(registrationId));
    }

    @Override
    public void notifyActivityCancelled(Long activityId) {
        send(mailContentFactory.activityCancelled(activityId));
    }

    @Override
    public void notifyActivityFinished(Long activityId) {
        send(mailContentFactory.activityFinished(activityId));
    }

    @Override
    public void notifyActivityClosed(Long activityId) {
        send(mailContentFactory.activityClosed(activityId));
    }

    @Override
    public void notifyActivitySubmittedForReview(Long activityId) {
        send(mailContentFactory.activitySubmittedForReview(activityId));
    }

    @Override
    public void notifyActivityApproved(Long activityId) {
        send(mailContentFactory.activityApproved(activityId));
    }

    @Override
    public void notifyActivityReturned(Long activityId) {
        send(mailContentFactory.activityReturned(activityId));
    }

    @Override
    public void notifyOrgAccountApproved(Long userId) {
        send(mailContentFactory.orgAccountApproved(userId));
    }

    @Override
    public void notifyOrgAccountRejected(Long userId) {
        send(mailContentFactory.orgAccountRejected(userId));
    }

    @Override
    public void notifyVerificationRequested(Long userId) {
        send(mailContentFactory.verificationRequested(userId));
    }

    /**
     * Difiere el envío al commit si hay transacción abierta; lo despacha en el
     * acto si no la hay, que es el caso de los tests y las tareas programadas.
     */
    private void send(List<MailMessage> messages) {
        for (MailMessage message : messages) {
            send(message);
        }
    }

    private void send(MailMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mailDispatcher.dispatch(message);
                }
            });
        } else {
            mailDispatcher.dispatch(message);
        }
    }
}
