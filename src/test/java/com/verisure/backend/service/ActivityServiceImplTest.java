package com.verisure.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.verisure.backend.dto.activity.ActivityFormResponse;
import com.verisure.backend.dto.activity.UpdateActivityRequest;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.mapper.ActivityMapperImpl;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.UserRepository;

/**
 * Las reglas de edición y cancelación de actividades · B2-05.
 *
 * <p>El mapper es el real (generado por MapStruct): no tiene sentido mockear la
 * copia de campos que el formulario envía. Los repositorios y el ciclo vital de
 * inscripciones, sí.
 */
class ActivityServiceImplTest {

    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RegistrationLifecycleService lifecycleService =
            mock(RegistrationLifecycleService.class);

    private ActivityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivityServiceImpl(
                activityRepository, new ActivityMapperImpl(), userRepository, lifecycleService);
    }

    private Activity activity(Long id, ActivityStatus status) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setTitle("Título original");
        activity.setStatus(status);
        activity.setPartner(new Partner());
        return activity;
    }

    private UpdateActivityRequest validRequest() {
        LocalDate start = LocalDate.of(2026, 10, 10);
        return new UpdateActivityRequest(
                "Título nuevo",
                "Descripción nueva",
                "educar",
                "PRESENCIAL",
                "Madrid",
                start,
                start.plusDays(1),
                start.minusDays(1),
                4,
                12);
    }

    @Test
    void update_appliesEditableFieldsAndPreservesStatusAndPartner() {
        Activity activity = activity(7L, ActivityStatus.PUBLISHED);
        Partner partner = new Partner();
        partner.setName("Cáritas");
        activity.setPartner(partner);
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(7L, validRequest());

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        Activity saved = captor.getValue();

        assertEquals("Título nuevo", saved.getTitle());
        assertEquals("Descripción nueva", saved.getDescription());
        assertEquals(ActivityStatus.PUBLISHED, saved.getStatus(), "el estado no lo toca el formulario");
        assertSame(partner, saved.getPartner(), "la entidad colaboradora no la toca el formulario");
        assertNull(saved.getReviewNote());
    }

    @Test
    void update_finishedThrowsActivityFinished() {
        Activity activity = activity(1L, ActivityStatus.FINISHED);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        DomainException ex = assertThrows(
                DomainException.class, () -> service.update(1L, validRequest()));

        assertEquals(ErrorCode.ACTIVITY_FINISHED, ex.getErrorCode());
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void update_cancelledThrowsActivityNotEditable() {
        Activity activity = activity(4L, ActivityStatus.CANCELLED);
        when(activityRepository.findById(4L)).thenReturn(Optional.of(activity));

        DomainException ex = assertThrows(
                DomainException.class, () -> service.update(4L, validRequest()));

        assertEquals(ErrorCode.ACTIVITY_NOT_EDITABLE, ex.getErrorCode());
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void update_missingThrowsNotFound() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(99L, validRequest()));
    }

    @Test
    void getForm_returnsAllFormFieldsInAnyState() {
        Activity activity = activity(4L, ActivityStatus.CANCELLED);
        activity.setDescription("Sigue visible para la administración");
        when(activityRepository.findById(4L)).thenReturn(Optional.of(activity));

        ActivityFormResponse form = service.getForm(4L);

        assertEquals(4L, form.id());
        assertEquals(ActivityStatus.CANCELLED, form.status());
        assertEquals("Sigue visible para la administración", form.description());
    }

    @Test
    void cancel_cascadesWithinTheSameCall() {
        Activity activity = activity(10L, ActivityStatus.PUBLISHED);
        when(activityRepository.findById(10L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancel(10L);

        verify(lifecycleService).cancelAllForActivity(10L);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        assertEquals(ActivityStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void cancel_finishedThrowsAndDoesNotCascade() {
        Activity activity = activity(1L, ActivityStatus.FINISHED);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        DomainException ex = assertThrows(DomainException.class, () -> service.cancel(1L));

        assertEquals(ErrorCode.ACTIVITY_FINISHED, ex.getErrorCode());
        verify(lifecycleService, never()).cancelAllForActivity(1L);
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void cancel_missingThrowsNotFound() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.cancel(99L));
    }
}