package com.verisure.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.repository.projection.SpotInfo;
import com.verisure.backend.support.TestData;

/** Plazas, cola y ascenso · B3-02 · B3-05. */
class SpotServiceImplTest {

    private static final Long ACTIVITY_ID = 7L;
    private static final String EMPLOYEE_EMAIL = "ana.gil@verisure.ex";

    private final RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private SpotServiceImpl service;
    private User employee;
    private Activity activity;

    @BeforeEach
    void setUp() {
        service = new SpotServiceImpl(registrationRepository, activityRepository, userRepository);
        employee = TestData.employee(10L, EMPLOYEE_EMAIL);
        activity = TestData.activity(ACTIVITY_ID, ActivityStatus.PUBLISHED, 2);

        when(userRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee));
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(activityRepository.getReferenceById(ACTIVITY_ID)).thenReturn(activity);
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void spotInfo(int spots, long confirmed, LocalDate deadline) {
        when(activityRepository.findSpotInfo(ACTIVITY_ID))
                .thenReturn(Optional.of(new SpotInfo(spots, confirmed, deadline)));
    }

    /** Responde como la consulta real: solo las que siguen en cola en ese momento. */
    private void queue(Registration... registrations) {
        when(registrationRepository.findByActivityIdAndStatusOrderByQueuePosition(
                ACTIVITY_ID, RegistrationStatus.WAITLISTED))
                .thenAnswer(invocation -> List.of(registrations).stream()
                        .filter(r -> r.getStatus() == RegistrationStatus.WAITLISTED)
                        .toList());
    }

    private Registration queued(Long id, boolean accepted, int position) {
        return TestData.registration(id, TestData.employee(id, id + "@verisure.ex"), activity,
                RegistrationStatus.WAITLISTED, accepted, position);
    }

    // register

    @Test
    void register_createsWaitlistedAtEndOfQueue() {
        spotInfo(2, 2, LocalDate.now().plusDays(1));
        when(registrationRepository.countByActivityIdAndStatus(ACTIVITY_ID, RegistrationStatus.WAITLISTED))
                .thenReturn(3L);

        Registration registration = service.register(ACTIVITY_ID, EMPLOYEE_EMAIL);

        assertEquals(RegistrationStatus.WAITLISTED, registration.getStatus());
        assertFalse(registration.isAccepted());
        assertEquals(4, registration.getQueuePosition());
        assertEquals(employee, registration.getUser());
        assertEquals(activity, registration.getActivity());
    }

    @Test
    void register_withLiveRegistration_throwsAlreadyRegistered() {
        spotInfo(2, 0, LocalDate.now().plusDays(1));
        when(registrationRepository.existsByActivityIdAndUserIdAndStatusNot(
                ACTIVITY_ID, employee.getId(), RegistrationStatus.CANCELLED)).thenReturn(true);

        DomainException ex = assertThrows(DomainException.class,
                () -> service.register(ACTIVITY_ID, EMPLOYEE_EMAIL));

        assertEquals(ErrorCode.ALREADY_REGISTERED, ex.getErrorCode());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void register_afterDeadline_throwsDeadlinePassed() {
        spotInfo(2, 0, LocalDate.now().minusDays(1));

        DomainException ex = assertThrows(DomainException.class,
                () -> service.register(ACTIVITY_ID, EMPLOYEE_EMAIL));

        assertEquals(ErrorCode.DEADLINE_PASSED, ex.getErrorCode());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void register_onDeadlineDay_isStillAllowed() {
        spotInfo(2, 0, LocalDate.now());

        Registration registration = service.register(ACTIVITY_ID, EMPLOYEE_EMAIL);

        assertEquals(RegistrationStatus.WAITLISTED, registration.getStatus());
    }

    @Test
    void register_missingActivity_throwsNotFound() {
        when(activityRepository.findSpotInfo(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.register(99L, EMPLOYEE_EMAIL));
    }

    // hasFreeSpot

    @Test
    void hasFreeSpot_whenConfirmedBelowSpots_isTrue() {
        spotInfo(2, 1, LocalDate.now());

        assertTrue(service.hasFreeSpot(ACTIVITY_ID));
    }

    @Test
    void hasFreeSpot_whenConfirmedEqualsSpots_isFalse() {
        spotInfo(2, 2, LocalDate.now());

        assertFalse(service.hasFreeSpot(ACTIVITY_ID));
    }

    // refreshFullStatus

    @Test
    void refreshFullStatus_lastSpotTaken_movesPublishedToFull() {
        spotInfo(2, 2, LocalDate.now());
        activity.setStatus(ActivityStatus.PUBLISHED);

        service.refreshFullStatus(ACTIVITY_ID);

        assertEquals(ActivityStatus.FULL, activity.getStatus());
    }

    @Test
    void refreshFullStatus_spotReleased_movesFullToPublished() {
        spotInfo(2, 1, LocalDate.now());
        activity.setStatus(ActivityStatus.FULL);

        service.refreshFullStatus(ACTIVITY_ID);

        assertEquals(ActivityStatus.PUBLISHED, activity.getStatus());
    }

    @Test
    void refreshFullStatus_inProgress_isNeverTouched() {
        activity.setStatus(ActivityStatus.IN_PROGRESS);

        spotInfo(2, 2, LocalDate.now());
        service.refreshFullStatus(ACTIVITY_ID);
        assertEquals(ActivityStatus.IN_PROGRESS, activity.getStatus());

        spotInfo(2, 0, LocalDate.now());
        service.refreshFullStatus(ACTIVITY_ID);
        assertEquals(ActivityStatus.IN_PROGRESS, activity.getStatus());
    }

    // promoteFirstInQueue

    @Test
    void promoteFirstInQueue_locksActivityBeforeReadingSpots() {
        spotInfo(2, 2, LocalDate.now());

        service.promoteFirstInQueue(ACTIVITY_ID);

        InOrder order = inOrder(activityRepository);
        order.verify(activityRepository).findByIdForUpdate(ACTIVITY_ID);
        order.verify(activityRepository).findSpotInfo(ACTIVITY_ID);
    }

    @Test
    void promoteFirstInQueue_withoutFreeSpot_returnsNullAndTouchesNothing() {
        spotInfo(2, 2, LocalDate.now());
        Registration accepted = queued(21L, true, 1);
        queue(accepted);

        Long promoted = service.promoteFirstInQueue(ACTIVITY_ID);

        assertNull(promoted);
        assertEquals(RegistrationStatus.WAITLISTED, accepted.getStatus());
        verify(registrationRepository, never()).findByActivityIdAndStatusOrderByQueuePosition(any(), any());
    }

    @Test
    void promoteFirstInQueue_promotesFirstAcceptedNotFirstInLine() {
        spotInfo(2, 1, LocalDate.now());
        Registration pending = queued(21L, false, 1);
        Registration accepted = queued(22L, true, 2);
        Registration later = queued(23L, true, 3);
        queue(pending, accepted, later);

        Long promoted = service.promoteFirstInQueue(ACTIVITY_ID);

        assertEquals(22L, promoted);
        assertEquals(RegistrationStatus.CONFIRMED, accepted.getStatus());
        assertNull(accepted.getQueuePosition());
        assertEquals(RegistrationStatus.WAITLISTED, pending.getStatus());
        assertEquals(1, pending.getQueuePosition());
        assertEquals(2, later.getQueuePosition(), "la cola se renumera sin el hueco");
    }

    @Test
    void promoteFirstInQueue_withoutAcceptedInQueue_returnsNull() {
        spotInfo(2, 1, LocalDate.now());
        queue(queued(21L, false, 1), queued(22L, false, 2));

        assertNull(service.promoteFirstInQueue(ACTIVITY_ID));
    }

    @Test
    void promoteFirstInQueue_missingActivity_throwsNotFound() {
        when(activityRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.promoteFirstInQueue(99L));
    }

    // reorderQueue

    @Test
    void reorderQueue_renumbersWithoutGaps() {
        Registration first = queued(21L, false, 2);
        Registration second = queued(22L, true, 5);
        Registration third = queued(23L, false, 9);
        queue(first, second, third);

        service.reorderQueue(ACTIVITY_ID);

        assertEquals(1, first.getQueuePosition());
        assertEquals(2, second.getQueuePosition());
        assertEquals(3, third.getQueuePosition());
    }

    @Test
    void register_savesTheBuiltRegistration() {
        spotInfo(2, 0, LocalDate.now().plusDays(1));

        service.register(ACTIVITY_ID, EMPLOYEE_EMAIL);

        ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getQueuePosition());
    }
}
