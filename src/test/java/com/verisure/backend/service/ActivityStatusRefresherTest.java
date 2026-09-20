package com.verisure.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.verisure.backend.dto.activity.RefreshStatusResponse;

/** La secuencia que comparten la tarea de las 03:00 y el endpoint de administración. */
class ActivityStatusRefresherTest {

    private final ActivityStatusService activityStatusService = mock(ActivityStatusService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ActivityStatusRefresher refresher =
            new ActivityStatusRefresher(activityStatusService, notificationService);

    @Test
    void refresh_startsBeforeFinishing() {
        when(activityStatusService.finishDueActivities()).thenReturn(List.of());

        refresher.refresh();

        InOrder order = inOrder(activityStatusService);
        order.verify(activityStatusService).startDueActivities();
        order.verify(activityStatusService).finishDueActivities();
    }

    @Test
    void refresh_notifiesOncePerFinishedActivity() {
        when(activityStatusService.startDueActivities()).thenReturn(1);
        when(activityStatusService.finishDueActivities()).thenReturn(List.of(3L, 4L));

        RefreshStatusResponse result = refresher.refresh();

        assertEquals(1, result.started());
        assertEquals(2, result.finished());
        verify(notificationService).notifyActivityFinished(3L);
        verify(notificationService).notifyActivityFinished(4L);
        verify(notificationService, times(2)).notifyActivityFinished(any());
    }

    @Test
    void refresh_withNothingFinished_doesNotNotify() {
        when(activityStatusService.finishDueActivities()).thenReturn(List.of());

        RefreshStatusResponse result = refresher.refresh();

        assertEquals(0, result.finished());
        verify(notificationService, never()).notifyActivityFinished(any());
    }
}
