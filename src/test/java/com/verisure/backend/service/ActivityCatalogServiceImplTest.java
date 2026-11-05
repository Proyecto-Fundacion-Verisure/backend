package com.verisure.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.activity.CatalogFilters;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.FavoriteRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ActivityCatalogServiceImplTest {

    @Mock ActivityRepository activityRepository;
    @Mock RegistrationRepository registrationRepository;
    @Mock FavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ActivityCatalogServiceImpl service;

    @Test
    void list_ordersAgainstTodaysDate() {
        Pageable pageable = PageRequest.of(0, 10);
        when(activityRepository.findCatalog(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        service.list(new CatalogFilters(null, null, null, null), pageable, "ana@ejemplo.ex");

        verify(activityRepository).findCatalog(
                any(), any(), any(), any(), any(), eq(LocalDate.now()), eq(pageable));
    }
}
