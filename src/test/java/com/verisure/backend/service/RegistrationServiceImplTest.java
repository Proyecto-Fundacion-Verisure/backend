package com.verisure.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.verisure.backend.dto.registration.CancelResult;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.support.TestData;

/** Decisión de la administradora y cancelación · B3-03 · B3-06. */
class RegistrationServiceImplTest {

    private static final Long REGISTRATION_ID = 5L;
    private static final Long ACTIVITY_ID = 7L;

    private final RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SpotService spotService = mock(SpotService.class);

    private RegistrationServiceImpl service;
    private User admin;
    private User owner;
    private User other;
    private Activity activity;
    private Registration registration;

    @BeforeEach
    void setUp() {
        service = new RegistrationServiceImpl(registrationRepository, userRepository, spotService);
        admin = TestData.admin();
        owner = TestData.employee(10L, "ana.gil@verisure.ex");
        other = TestData.employee(11L, "carlos.pena@verisure.ex");
        activity = TestData.activity(ACTIVITY_ID, ActivityStatus.FULL, 2);
        registration = TestData.registration(REGISTRATION_ID, owner, activity,
                RegistrationStatus.WAITLISTED, false, 3);

        when(registrationRepository.findByIdWithActivity(REGISTRATION_ID))
                .thenReturn(Optional.of(registration));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));
    }

    // accept

    @Test
    void accept_withFreeSpot_confirmsAndLeavesQueue() {
        when(spotService.hasFreeSpot(ACTIVITY_ID)).thenReturn(true);

        Registration result = service.accept(REGISTRATION_ID, admin.getEmail());

        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertTrue(result.isAccepted());
        assertNull(result.getQueuePosition());
        assertSame(admin, result.getDecidedBy());
        assertNotNull(result.getDecidedAt());
        verify(spotService).reorderQueue(ACTIVITY_ID);
        verify(spotService).refreshFullStatus(ACTIVITY_ID);
    }

    @Test
    void accept_withoutFreeSpot_staysWaitlistedAcceptedAndNeverThrows() {
        when(spotService.hasFreeSpot(ACTIVITY_ID)).thenReturn(false);

        Registration result = service.accept(REGISTRATION_ID, admin.getEmail());

        assertEquals(RegistrationStatus.WAITLISTED, result.getStatus());
        assertTrue(result.isAccepted());
        assertEquals(3, result.getQueuePosition(), "conserva su puesto en la cola");
        assertSame(admin, result.getDecidedBy());
        verify(spotService, never()).reorderQueue(any());
        verify(spotService, never()).refreshFullStatus(any());
    }

    @Test
    void accept_missingRegistration_throwsNotFound() {
        when(registrationRepository.findByIdWithActivity(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.accept(99L, admin.getEmail()));
    }

    // reject

    @Test
    void reject_marksRejectedAndReordersQueue() {
        Registration result = service.reject(REGISTRATION_ID, admin.getEmail());

        assertEquals(RegistrationStatus.REJECTED, result.getStatus());
        assertFalse(result.isAccepted());
        assertNull(result.getQueuePosition());
        assertSame(admin, result.getDecidedBy());
        verify(spotService).reorderQueue(ACTIVITY_ID);
    }

    // cancel

    @Test
    void cancel_byOwner_cancelsAndReturnsPromotedId() {
        registration.setStatus(RegistrationStatus.CONFIRMED);
        when(spotService.promoteFirstInQueue(ACTIVITY_ID)).thenReturn(22L);

        CancelResult result = service.cancel(REGISTRATION_ID, owner.getEmail(), "no puedo ir");

        assertEquals(RegistrationStatus.CANCELLED, registration.getStatus());
        assertNull(registration.getQueuePosition());
        assertEquals(RegistrationStatus.CANCELLED, result.body().status());
        assertEquals(22L, result.promotedRegistrationId());
    }

    @Test
    void cancel_withNobodyToPromote_stillRefreshesStatusAndQueue() {
        when(spotService.promoteFirstInQueue(ACTIVITY_ID)).thenReturn(null);

        CancelResult result = service.cancel(REGISTRATION_ID, owner.getEmail(), null);

        assertNull(result.promotedRegistrationId());
        verify(spotService).refreshFullStatus(ACTIVITY_ID);
        verify(spotService).reorderQueue(ACTIVITY_ID);
    }

    @Test
    void cancel_byThirdParty_throwsNotOwnerBeforeCheckingDates() {
        activity.setStartDate(LocalDate.now().minusDays(5));

        DomainException ex = assertThrows(DomainException.class,
                () -> service.cancel(REGISTRATION_ID, other.getEmail(), null));

        assertEquals(ErrorCode.NOT_OWNER, ex.getErrorCode());
        assertEquals(RegistrationStatus.WAITLISTED, registration.getStatus());
        verify(spotService, never()).promoteFirstInQueue(any());
    }

    @Test
    void cancel_byOwnerAfterStart_throwsDeadlinePassed() {
        activity.setStartDate(LocalDate.now().minusDays(1));

        DomainException ex = assertThrows(DomainException.class,
                () -> service.cancel(REGISTRATION_ID, owner.getEmail(), null));

        assertEquals(ErrorCode.DEADLINE_PASSED, ex.getErrorCode());
        assertEquals(RegistrationStatus.WAITLISTED, registration.getStatus());
    }

    @Test
    void cancel_byOwnerOnStartDay_isStillAllowed() {
        activity.setStartDate(LocalDate.now());

        service.cancel(REGISTRATION_ID, owner.getEmail(), null);

        assertEquals(RegistrationStatus.CANCELLED, registration.getStatus());
    }

    @Test
    void cancel_byAdminAfterStart_isAllowed() {
        activity.setStartDate(LocalDate.now().minusDays(1));

        service.cancel(REGISTRATION_ID, admin.getEmail(), "baja de última hora");

        assertEquals(RegistrationStatus.CANCELLED, registration.getStatus());
        verify(spotService).promoteFirstInQueue(ACTIVITY_ID);
    }
}
