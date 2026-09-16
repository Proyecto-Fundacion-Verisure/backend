package com.verisure.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.orgaccount.OrgAccountRow;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.PartnerStatus;
import com.verisure.backend.entity.enums.UserStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementación de {@link OrgAccountService} · {@code B1-16}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgAccountServiceImpl implements OrgAccountService {

    private final UserRepository userRepository;

    private static final List<UserStatus> PENDING_STATUSES =
            List.of(UserStatus.PENDING_VERIFICATION, UserStatus.PENDING_APPROVAL);

    @Override
    public Page<OrgAccountRow> list(String status, Pageable pageable) {
        return userRepository.findAccounts(resolveStatuses(status), pageable);
    }

    @Override
    @Transactional
    public OrgAccountRow approve(Long userId) {
        User user = findOrThrow(userId);
        user.setStatus(UserStatus.ACTIVE);

        Partner partner = user.getPartner();
        if (partner != null && partner.getStatus() != PartnerStatus.ACTIVE) {
            partner.setStatus(PartnerStatus.ACTIVE);
        }

        return toRow(userRepository.save(user));
    }

    @Override
    @Transactional
    public OrgAccountRow reject(Long userId) {
        User user = findOrThrow(userId);
        Partner partner = user.getPartner();

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);

        // La consulta de abajo se ejecuta tras el flush: el usuario ya no
        // cuenta como activo dentro de su entidad.
        if (partner != null
                && partner.getStatus() == PartnerStatus.PENDING
                && userRepository.countByPartnerIdAndStatus(partner.getId(), UserStatus.ACTIVE) == 0) {
            partner.setStatus(PartnerStatus.REJECTED);
        }

        return toRow(user);
    }

    private List<UserStatus> resolveStatuses(String status) {
        if (status == null || status.isBlank() || status.equals("PENDING")) {
            return PENDING_STATUSES;
        }
        try {
            return List.of(UserStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR,
                    "Estado de cuenta no válido: " + status);
        }
    }

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("User", userId));
    }

    private OrgAccountRow toRow(User user) {
        Partner partner = user.getPartner();
        return new OrgAccountRow(
                user.getId(),
                partner != null ? partner.getName() : null,
                partner != null ? partner.getCif() : null,
                user.getFullName(),
                user.getEmail(),
                partner != null ? partner.getPhone() : null,
                user.getCreatedAt() != null ? user.getCreatedAt()
                        : (partner != null ? partner.getCreatedAt() : null),
                user.getStatus(),
                user.getVerifiedAt() != null);
    }
}