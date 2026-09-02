package com.verisure.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.closure.CertificateResponse;
import com.verisure.backend.repository.ParticipationClosureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final ParticipationClosureRepository participationClosureRepository;

    @Override
    @Transactional(readOnly = true)
    public CertificateResponse generate(Long closureId) {
        return null; // TODO B1-06
    }
}
