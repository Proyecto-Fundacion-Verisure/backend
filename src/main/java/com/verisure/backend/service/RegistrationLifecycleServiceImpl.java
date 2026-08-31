package com.verisure.backend.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationLifecycleServiceImpl implements RegistrationLifecycleService {

@Override @Transactional
public int cancelAllForActivity(Long activityId) { return 0; }

@Override @Transactional
public int closeAllForActivity(Long activityId) { return 0; }

}
