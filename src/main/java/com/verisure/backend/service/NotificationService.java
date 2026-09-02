package com.verisure.backend.service;

/**
 * Avisos por correo del sistema. Sustituye a los eventos de Spring.
 *
 * <p><b>REGLA · el aviso se llama SIEMPRE fuera de la transacción.</b> Orquesta
 * quien llama al servicio transaccional: el controlador en los endpoints, y el
 * método {@code @Scheduled} en las tareas programadas —que por eso no llevan
 * {@code @Transactional}—. Llamar a estos métodos dentro de un método
 * {@code @Transactional} manda el correo aunque la operación se deshaga después,
 * y no hay compilador que lo detecte.
 *
 * <p>Como red de seguridad, {@link NotificationServiceImpl} difiere el envío a
 * {@code afterCommit()} si detecta una transacción abierta. Es una protección,
 * no un permiso para saltarse la regla: el orden correcto sigue siendo servicio
 * primero, aviso después.
 *
 * <p>Hay un caso que la regla no cubre por sí sola: cuando el destinatario
 * <i>nace</i> dentro de la transacción y su identificador no está en la ruta,
 * como en {@link #notifySpotReleased(Long)}, que va a quien ascendió de la
 * cola. Ahí el servicio tiene que devolver ese identificador para que el
 * controlador pueda avisar.
 *
 * <p>Cubre los siete avisos de participación de H23 (Épica 7) más los del rol
 * de entidad. Las plantillas se escriben en B3-09, B3-13 y B3-14.
 */
public interface NotificationService {

    // Participación · las llama BE3

    /** Plaza confirmada. */
    void notifyRegistrationConfirmed(Long registrationId);

    /** Solicitud recibida y en lista de espera, con su posición. */
    void notifyRegistrationWaitlisted(Long registrationId);

    /** Inscripción rechazada. Sin motivo, por contrato. */
    void notifyRegistrationRejected(Long registrationId);

    /**
     * Plaza liberada: quien estaba en cabeza de la cola ha ascendido.
     * El identificador es el de <b>esa</b> inscripción, no el de la que se canceló.
     */
    void notifySpotReleased(Long registrationId);

    // Actividad · las llaman BE1 y BE2

    /** La actividad se ha cancelado. Va a todas sus inscripciones vivas. */
    void notifyActivityCancelled(Long activityId);

    /** La actividad ha terminado: «cuéntanos cómo fue». Lo dispara la tarea B3-17. */
    void notifyActivityFinished(Long activityId);

    /** La Fundación ha cerrado la actividad. Lleva el enlace al certificado. */
    void notifyActivityClosed(Long activityId);

    // Rol entidad · las llaman BE1 y BE2

    /** Una entidad ha enviado una actividad a revisión. Va a administración. */
    void notifyActivitySubmittedForReview(Long activityId);

    /** La Fundación ha aprobado la actividad propuesta por la entidad. */
    void notifyActivityApproved(Long activityId);

    /** La Fundación devuelve la actividad a la entidad. Lleva el {@code reviewNote}. */
    void notifyActivityReturned(Long activityId);

    /** Cuenta de entidad aprobada. Lleva el enlace al panel. */
    void notifyOrgAccountApproved(Long userId);

    /** Cuenta de entidad rechazada. Sin motivo detallado, igual que el rechazo de inscripción. */
    void notifyOrgAccountRejected(Long userId);

    /**
     * Enlace de verificación de correo, con su caducidad. Lo llama B1-15 desde
     * {@code /api/auth/verify} y {@code /api/auth/resend-verification}.
     *
     * <p>Es <b>el único que no puede fallar en silencio</b>: si no llega, la
     * entidad se queda bloqueada sin saber por qué. El fallo se registra con
     * nivel de aviso, no de información.
     */
    void notifyVerificationRequested(Long userId);
}
