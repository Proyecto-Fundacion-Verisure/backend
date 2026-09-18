package com.verisure.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.entity.enums.UserStatus;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.service.AuthService;
import com.verisure.backend.service.NotificationService;
import com.verisure.backend.support.ControllerTest;
import com.verisure.backend.support.TestData;

/** {@code POST /api/auth/login} de punta a punta, con el filtro real y BCrypt real. */
@ControllerTest(AuthController.class)
class AuthControllerIT {

    private static final String EMAIL = "marta.ribas@caritasbcn.ex";

    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean UserRepository userRepository;
    @MockitoBean AuthService authService;
    @MockitoBean NotificationService notificationService;

    private void accountWithStatus(UserStatus status) {
        User user = TestData.user(9L, EMAIL, Role.PARTNER, status);
        user.setPassword(passwordEncoder.encode(TestData.PASSWORD));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private ResultActions login(String body) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String credentials(String password) {
        return "{\"email\":\"" + EMAIL + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void login_activeAccount_returnsTokenAndUser() throws Exception {
        accountWithStatus(UserStatus.ACTIVE);

        login(credentials(TestData.PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(7200))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("PARTNER"))
                .andExpect(jsonPath("$.user.organization").doesNotExist());
    }

    @Test
    void login_wrongPassword_is401Generic() throws Exception {
        accountWithStatus(UserStatus.ACTIVE);

        login(credentials("otra"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void login_unknownEmail_is401WithSameMessage() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        login(credentials(TestData.PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales no válidas"));
    }

    @Test
    void login_pendingApproval_is403WithAccountCode() throws Exception {
        accountWithStatus(UserStatus.PENDING_APPROVAL);

        login(credentials(TestData.PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING_APPROVAL"));
    }

    @Test
    void login_rejectedWithWrongPassword_is401NotAccountRejected() throws Exception {
        accountWithStatus(UserStatus.REJECTED);

        login(credentials("otra"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void login_unreadableBody_is400MalformedRequest() throws Exception {
        login("esto no es json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
