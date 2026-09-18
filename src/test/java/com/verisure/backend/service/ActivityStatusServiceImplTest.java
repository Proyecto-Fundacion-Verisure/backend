package com.verisure.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.support.TestData;

/** Paso de estados por fecha · B3-17. */
class ActivityStatusServiceImplTest {

    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final RegistrationRepository registrationRepository = mock(RegistrationRepository.class);

    private ActivityStatusServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivityStatusServiceImpl(activityRepository, registrationRepository);
        when(activityRepository.findByStatusInAndStartDateLessThanEqual(anyList(), any()))
                .thenReturn(List.of());
        when(activityRepository.findByStatusAndEndDateBefore(any(), any()))
                .thenReturn(List.of());
        when(registrationRepository.findByActivityIdAndStatusIn(any(), anyList()))
                .thenReturn(List.of());
    }

    @Test
    void startDueActivities_movesEveryDueOneToInProgress() {
        Activity published = TestData.activity(1L, ActivityStatus.PUBLISHED, 2);
        Activity full = TestData.activity(2L, ActivityStatus.FULL, 2);
        when(activityRepository.findByStatusInAndStartDateLessThanEqual(anyList(), any()))
                .thenReturn(List.of(published, full));

        int started = service.startDueActivities();

        assertEquals(2, started);
        assertEquals(ActivityStatus.IN_PROGRESS, published.getStatus());
        assertEquals(ActivityStatus.IN_PROGRESS, full.getStatus());
    }

    @Test
    void startDueActivities_withNothingDue_returnsZero() {
        assertEquals(0, service.startDueActivities());
    }

    @Test
    void finishDueActivities_finishesAndMovesRegistrations() {
        Activity activity = TestData.activity(3L, ActivityStatus.IN_PROGRESS, 2);
        Registration confirmed = TestData.registration(31L, TestData.employee(1L, "a@verisure.ex"),
                activity, RegistrationStatus.CONFIRMED, true, null);
        Registration queued = TestData.registration(32L, TestData.employee(2L, "b@verisure.ex"),
                activity, RegistrationStatus.WAITLISTED, true, 1);
        when(activityRepository.findByStatusAndEndDateBefore(eq(ActivityStatus.IN_PROGRESS), any()))
                .thenReturn(List.of(activity));
        when(registrationRepository.findByActivityIdAndStatusIn(3L, List.of(RegistrationStatus.CONFIRMED)))
                .thenReturn(List.of(confirmed));
        when(registrationRepository.findByActivityIdAndStatusIn(3L, List.of(RegistrationStatus.WAITLISTED)))
                .thenReturn(List.of(queued));

        List<Long> finished = service.finishDueActivities();

        assertEquals(List.of(3L), finished);
        assertEquals(ActivityStatus.FINISHED, activity.getStatus());
        assertEquals(RegistrationStatus.PENDING_CLOSURE, confirmed.getStatus());
        assertEquals(RegistrationStatus.CANCELLED, queued.getStatus());
        assertNull(queued.getQueuePosition());
    }

    @Test
    void finishDueActivities_withNothingDue_returnsEmptyList() {
        assertTrue(service.finishDueActivities().isEmpty());
    }
}
