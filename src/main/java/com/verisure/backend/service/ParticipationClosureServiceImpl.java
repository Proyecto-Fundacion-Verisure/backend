package com.verisure.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.closure.ClosureDetailResponse;
import com.verisure.backend.dto.closure.CreateClosureRequest;
import com.verisure.backend.repository.ParticipationClosureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipationClosureServiceImpl implements ParticipationClosureService {

    private final ParticipationClosureRepository participationClosureRepository;

    @Override
    @Transactional
    public ClosureDetailResponse submit(CreateClosureRequest request) {
        return null; // TODO B1-03
    }

    @Override
    @Transactional(readOnly = true)
    public ClosureDetailResponse getById(Long closureId) {
        return null; // TODO B1-03
    }
}
