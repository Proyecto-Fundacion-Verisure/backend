package com.verisure.backend.service;

import org.springframework.stereotype.Service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService  {

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
        send("actividad finalizada", "actividad", activityId);

    }

    @Override
    public void notifyActivityClosed(Long activityId) {
        send("colaboración cerrada", "actividad", activityId);
    }

    @Override
    public void notifyActivitySubmittedForReview(Long activityId) {
        send("actividad enviada a revisión", "actividad", activityId);
    }

    @Override
    public void notifyActivityApproved(Long activityId) {
        send("actividad aprobada", "actividad", activityId);

    }
    
    public void notifyActivityReturned(Long activityId){
        send("actividad pendiente de cambios", "actividad", activityId);
    }
    //Revisar o darle una vuelta al pendiente de cambios, por si hay una mejor manera de decirlo

    @Override
    public void notifyOrgAccountApproved(Long userId) {
        send("cuenta aprobada", "usuario", userId);
    }


    @Override
    public void notifyOrgAccountRejected(Long userId) {
        send("cuenta rechazada", "usuario", userId);
    }


    private void send(String subject, String kind, Long id) throws IllegalStateException {
        Runnable task = () -> {
            try {
                // TODO B3-09 · sustituir por mailService.send(...) con su plantilla
                log.info("TODO B3-09 · correo «{}» para {} {}", subject, kind, id);
            } catch (Exception e) {
                log.warn("No se pudo enviar «{}» para {} {}: {}", subject, kind, id, e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }


}
