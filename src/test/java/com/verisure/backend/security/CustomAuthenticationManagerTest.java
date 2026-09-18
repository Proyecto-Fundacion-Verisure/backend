package com.verisure.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.entity.enums.UserStatus;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.support.TestData;

/** Credenciales primero, estado de la cuenta después · C-04. */
class CustomAuthenticationManagerTest {

    private static final String EMAIL = "marta.ribas@caritasbcn.ex";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private CustomAuthenticationManager manager;

    @BeforeEach
    void setUp() {
        manager = new CustomAuthenticationManager(userRepository, passwordEncoder);
    }

    private User userWithStatus(UserStatus status) {
        User user = TestData.user(1L, EMAIL, Role.PARTNER, status);
        user.setPassword(passwordEncoder.encode(TestData.PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        return user;
    }

    private Authentication attempt(String password) {
        return manager.authenticate(new UsernamePasswordAuthenticationToken(EMAIL, password));
    }

    @Test
    void authenticate_activeAccountWithRightPassword_returnsUserWithRole() {
        userWithStatus(UserStatus.ACTIVE);

        Authentication result = attempt(TestData.PASSWORD);

        assertTrue(result.isAuthenticated());
        assertEquals(EMAIL, ((UserDetail) result.getPrincipal()).getUsername());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARTNER")));
    }

    @Test
    void authenticate_wrongPassword_throwsBadCredentialsEvenIfRejected() {
        userWithStatus(UserStatus.REJECTED);

        assertThrows(BadCredentialsException.class, () -> attempt("otra"));
    }

    @Test
    void authenticate_unknownEmail_throwsSameErrorAsWrongPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        BadCredentialsException unknown = assertThrows(BadCredentialsException.class,
                () -> attempt(TestData.PASSWORD));

        userWithStatus(UserStatus.ACTIVE);
        BadCredentialsException wrong = assertThrows(BadCredentialsException.class,
                () -> attempt("otra"));

        assertEquals(unknown.getMessage(), wrong.getMessage(), "no se revela si el correo existe");
    }

    @Test
    void authenticate_pendingVerification_throwsAccountNotVerified() {
        userWithStatus(UserStatus.PENDING_VERIFICATION);

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> attempt(TestData.PASSWORD));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
    }

    @Test
    void authenticate_pendingApproval_throwsAccountPendingApproval() {
        userWithStatus(UserStatus.PENDING_APPROVAL);

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> attempt(TestData.PASSWORD));

        assertEquals(ErrorCode.ACCOUNT_PENDING_APPROVAL, ex.getErrorCode());
    }

    @Test
    void authenticate_rejected_throwsAccountRejected() {
        userWithStatus(UserStatus.REJECTED);

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> attempt(TestData.PASSWORD));

        assertEquals(ErrorCode.ACCOUNT_REJECTED, ex.getErrorCode());
    }
}
