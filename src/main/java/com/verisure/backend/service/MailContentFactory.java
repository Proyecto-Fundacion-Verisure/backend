package com.verisure.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.repository.projection.ActivityMailView;
import com.verisure.backend.repository.projection.RegistrationMailView;
import com.verisure.backend.repository.projection.UserMailView;

/**
 * Convierte un aviso en los correos concretos que hay que enviar.
 *
 * <p>Devuelve siempre una lista porque hay avisos que van a varias personas, y
 * una lista vacía cuando el destinatario ya no existe: un aviso sobre una fila
 * borrada no puede tumbar la operación que lo disparó.
 *
 * <p>Las rutas de frontend viven aquí como constantes y se montan sobre
 * {@code app.base-url}, para que ninguna plantilla lleve un dominio dentro.
 *
 * <p>Dueña: BE3 · Tarea: B3-09.
 */
@Component
public class MailContentFactory {

    private static final String MY_VOLUNTEERING = "/my-volunteering";
    private static final String ACTIVITY = "/activities/";
    private static final String CLOSURE = "/closures/";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    /** Estados con plaza o con opción a ella: los que reciben el aviso de cancelación. */
    private static final List<RegistrationStatus> LIVE_STATUSES =
            List.of(RegistrationStatus.WAITLISTED, RegistrationStatus.CONFIRMED);

    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final String baseUrl;

    public MailContentFactory(RegistrationRepository registrationRepository,
                              ActivityRepository activityRepository,
                              UserRepository userRepository,
                              @Value("${app.base-url}") String baseUrl) {
        this.registrationRepository = registrationRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.baseUrl = baseUrl;
    }

    // Participación

    public List<MailMessage> registrationConfirmed(Long registrationId) {
        return forRegistration(registrationId, "Plaza confirmada",
                "mail/registration-confirmed", link(MY_VOLUNTEERING));
    }

    /**
     * Lleva la posición en la cola, que es el dato que la persona quiere ver.
     *
     * <p>No usa {@link #forRegistration} porque esa posición solo se conoce
     * después de buscar la fila.
     */
    public List<MailMessage> registrationWaitlisted(Long registrationId) {
        RegistrationMailView view = findRegistration(registrationId);
        if (view == null) {
            return List.of();
        }

        Map<String, Object> variables = variablesOf(view, link(MY_VOLUNTEERING));
        variables.put("queuePosition", view.queuePosition());

        return List.of(MailMessage.of(view.userEmail(),
                subject("Estás en lista de espera", view.activityTitle()),
                "mail/registration-waitlisted", variables));
    }

    /**
     * Manda a la ficha de la actividad y no a «mis voluntariados», porque ahí ya
     * no hay nada suyo. Por eso tampoco usa {@link #forRegistration}: el enlace
     * necesita el identificador de la actividad, que llega con la fila.
     */
    public List<MailMessage> registrationRejected(Long registrationId) {
        RegistrationMailView view = findRegistration(registrationId);
        if (view == null) {
            return List.of();
        }

        return List.of(MailMessage.of(view.userEmail(),
                subject("Solicitud no admitida", view.activityTitle()),
                "mail/registration-rejected",
                variablesOf(view, link(ACTIVITY + view.activityId()))));
    }

    public List<MailMessage> spotReleased(Long registrationId) {
        return forRegistration(registrationId, "Ha quedado una plaza libre y es tuya",
                "mail/spot-released", link(MY_VOLUNTEERING));
    }

    // Actividad · va a varias personas

    public List<MailMessage> activityCancelled(Long activityId) {
        return forActivityRegistrations(activityId, LIVE_STATUSES,
                "Actividad cancelada", "mail/activity-cancelled");
    }

    public List<MailMessage> activityFinished(Long activityId) {
        return forActivityRegistrations(activityId, List.of(RegistrationStatus.PENDING_CLOSURE),
                "Cuéntanos cómo fue", "mail/activity-finished");
    }

    /** Cada destinatario recibe el enlace a <b>su</b> cierre, no a la actividad. */
    public List<MailMessage> activityClosed(Long activityId) {
        List<RegistrationMailView> views = registrationRepository
                .findMailViewsByActivityIdAndStatusIn(activityId, List.of(RegistrationStatus.CLOSED));

        List<MailMessage> messages = new ArrayList<>();
        for (RegistrationMailView view : views) {
            messages.add(MailMessage.of(view.userEmail(),
                    subject("Certificado disponible", view.activityTitle()),
                    "mail/activity-closed",
                    variablesOf(view, link(CLOSURE + view.closureId()))));
        }
        return messages;
    }

    // Rol entidad

    /** Va a toda la administración: quien revise primero, revisa. */
    public List<MailMessage> activitySubmittedForReview(Long activityId) {
        ActivityMailView activity = findActivity(activityId);
        if (activity == null) {
            return List.of();
        }

        List<MailMessage> messages = new ArrayList<>();
        for (UserMailView admin : userRepository.findMailViewsByRole(Role.ADMIN)) {
            Map<String, Object> variables = variablesOf(activity, link(ACTIVITY + activityId));
            // Es el único aviso de actividad que no va a la entidad, así que el
            // saludo se reemplaza por el nombre de quien lo recibe.
            variables.put("userName", admin.userName());
            messages.add(MailMessage.of(admin.userEmail(),
                    subject("Actividad pendiente de revisión", activity.title()),
                    "mail/activity-submitted-for-review", variables));
        }
        return messages;
    }

    public List<MailMessage> activityApproved(Long activityId) {
        return forActivityContact(activityId, "Actividad aprobada y publicada",
                "mail/activity-approved");
    }

    /** Lleva el {@code reviewNote} íntegro: si hay que entrar a leerlo, la mitad no entra. */
    public List<MailMessage> activityReturned(Long activityId) {
        return forActivityContact(activityId, "Actividad devuelta con comentarios",
                "mail/activity-returned");
    }

    // Cuenta

    public List<MailMessage> orgAccountApproved(Long userId) {
        return forUser(userId, "Tu cuenta ya está activa",
                "mail/org-account-approved", baseUrl);
    }

    public List<MailMessage> orgAccountRejected(Long userId) {
        return forUser(userId, "No hemos podido activar tu cuenta",
                "mail/org-account-rejected", baseUrl);
    }

    /**
     * El enlace apunta a la pantalla de estado de la cuenta y lleva el token;
     * la caducidad se lee del {@code createdAt} del token · {@code B1-15}.
     *
     * <p>Si la cuenta no tiene token (las sembradas de la demo), se mandan
     * huecos y la plantilla avisa de que el enlace todavía no está disponible.
     */
    public List<MailMessage> verificationRequested(Long userId) {
        UserMailView user = findUser(userId);
        if (user == null) {
            return List.of();
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", user.userName());
        String token = user.verificationToken();
        Instant createdAt = user.verificationCreatedAt();
        if (token != null && createdAt != null) {
            variables.put("verificationLink",
                    link("/account-status?token=" + token));
            variables.put("expiresAt", format(
                    LocalDateTime.ofInstant(createdAt.plus(User.VERIFICATION_TTL),
                            ZoneId.systemDefault())));
        } else {
            variables.put("verificationLink", "");
            variables.put("expiresAt", "");
        }

        return List.of(new MailMessage(user.userEmail(), "Verifica tu correo",
                "mail/verification-requested", variables, true));
    }

    // Plantillas de resolución, compartidas por varios avisos

    private List<MailMessage> forRegistration(Long registrationId, String subjectText,
                                              String template, String link) {
        RegistrationMailView view = findRegistration(registrationId);
        if (view == null) {
            return List.of();
        }

        return List.of(MailMessage.of(view.userEmail(),
                subject(subjectText, view.activityTitle()), template, variablesOf(view, link)));
    }

    private List<MailMessage> forActivityRegistrations(Long activityId,
                                                       List<RegistrationStatus> statuses,
                                                       String subjectText, String template) {
        List<RegistrationMailView> views = registrationRepository
                .findMailViewsByActivityIdAndStatusIn(activityId, statuses);

        List<MailMessage> messages = new ArrayList<>();
        for (RegistrationMailView view : views) {
            messages.add(MailMessage.of(view.userEmail(),
                    subject(subjectText, view.activityTitle()), template,
                    variablesOf(view, link(ACTIVITY + activityId))));
        }
        return messages;
    }

    private List<MailMessage> forActivityContact(Long activityId, String subjectText, String template) {
        ActivityMailView activity = findActivity(activityId);
        if (activity == null) {
            return List.of();
        }

        return List.of(MailMessage.of(activity.contactEmail(),
                subject(subjectText, activity.title()), template,
                variablesOf(activity, link(ACTIVITY + activityId))));
    }

    private List<MailMessage> forUser(Long userId, String subjectText, String template, String link) {
        UserMailView user = findUser(userId);
        if (user == null) {
            return List.of();
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", user.userName());
        variables.put("link", link);

        return List.of(MailMessage.of(user.userEmail(), subjectText, template, variables));
    }

    private Map<String, Object> variablesOf(RegistrationMailView view, String link) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", view.userName());
        variables.put("activityTitle", view.activityTitle());
        variables.put("startDate", format(view.startDate()));
        variables.put("endDate", format(view.endDate()));
        variables.put("link", link);
        return variables;
    }

    private Map<String, Object> variablesOf(ActivityMailView activity, String link) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", activity.contactName());
        variables.put("activityTitle", activity.title());
        variables.put("partnerName", activity.partnerName());
        variables.put("reviewNote", activity.reviewNote());
        variables.put("link", link);
        return variables;
    }

    private RegistrationMailView findRegistration(Long registrationId) {
        Optional<RegistrationMailView> view = registrationRepository.findMailViewById(registrationId);
        return view.orElse(null);
    }

    private ActivityMailView findActivity(Long activityId) {
        Optional<ActivityMailView> view = activityRepository.findMailViewById(activityId);
        return view.orElse(null);
    }

    private UserMailView findUser(Long userId) {
        Optional<UserMailView> view = userRepository.findMailViewById(userId);
        return view.orElse(null);
    }

    private String subject(String text, String activityTitle) {
        return text + " · " + activityTitle;
    }

    private String link(String path) {
        return baseUrl + path;
    }

    private String format(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE);
    }

    private String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME);
    }
}
