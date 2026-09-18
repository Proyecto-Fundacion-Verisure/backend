package com.verisure.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.service.NotificationService;
import com.verisure.backend.service.RegistrationService;
import com.verisure.backend.service.SpotService;
import com.verisure.backend.support.AuthHelper;
import com.verisure.backend.support.ControllerTest;

/** La forma única {@code ApiError} para cada familia de error · C-03. */
@ControllerTest(RegistrationController.class)
class GlobalExceptionHandlerIT {

    private static final String AUTHORIZATION = "Authorization";

    @Autowired MockMvc mockMvc;
    @Autowired AuthHelper auth;

    @MockitoBean UserRepository userRepository;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean SpotService spotService;
    @MockitoBean NotificationService notificationService;

    private ResultActions expectApiErrorShape(ResultActions result, String code, String path) throws Exception {
        return result
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value(path));
    }

    @Test
    void beanValidationFailure_is400WithFields() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/registrations")
                        .header(AUTHORIZATION, auth.bearer(Role.EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.activityId").isArray());

        expectApiErrorShape(result, "VALIDATION_ERROR", "/api/registrations");
    }

    @Test
    void unreadableBody_is400MalformedRequest() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/registrations")
                        .header(AUTHORIZATION, auth.bearer(Role.EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("esto no es json"))
                .andExpect(status().isBadRequest());

        expectApiErrorShape(result, "MALFORMED_REQUEST", "/api/registrations");
    }

    @Test
    void invalidEnumParameter_is400MalformedRequestNamingTheParameter() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/admin/registrations")
                        .header(AUTHORIZATION, auth.bearer(Role.ADMIN))
                        .param("status", "LO_QUE_SEA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.status").isArray());

        expectApiErrorShape(result, "MALFORMED_REQUEST", "/api/admin/registrations");
    }

    @Test
    void unknownRoute_is404NotFound() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/no-existe")
                        .header(AUTHORIZATION, auth.bearer(Role.ADMIN)))
                .andExpect(status().isNotFound());

        expectApiErrorShape(result, "NOT_FOUND", "/api/no-existe");
    }

    @Test
    void unsupportedMethod_is405() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/registrations/me")
                        .header(AUTHORIZATION, auth.bearer(Role.EMPLOYEE)))
                .andExpect(status().isMethodNotAllowed());

        expectApiErrorShape(result, "METHOD_NOT_ALLOWED", "/api/registrations/me");
    }

    @Test
    void domainException_usesItsOwnHttpStatusAndCode() throws Exception {
        when(spotService.register(anyLong(), any()))
                .thenThrow(new DomainException(ErrorCode.ALREADY_REGISTERED));

        ResultActions result = mockMvc.perform(post("/api/registrations")
                        .header(AUTHORIZATION, auth.bearer(Role.EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\": 7}"))
                .andExpect(status().isConflict());

        expectApiErrorShape(result, "ALREADY_REGISTERED", "/api/registrations");
    }

    @Test
    void notFoundException_is404() throws Exception {
        when(registrationService.accept(anyLong(), any()))
                .thenThrow(NotFoundException.of("inscripción", 99L));

        ResultActions result = mockMvc.perform(patch("/api/registrations/99/accept")
                        .header(AUTHORIZATION, auth.bearer(Role.ADMIN)))
                .andExpect(status().isNotFound());

        expectApiErrorShape(result, "NOT_FOUND", "/api/registrations/99/accept");
    }

    @Test
    void unexpectedException_is500WithoutTrace() throws Exception {
        when(registrationService.getCounts(anyLong()))
                .thenThrow(new IllegalStateException("detalle interno que no debe salir"));

        ResultActions result = mockMvc.perform(get("/api/admin/registrations/counts")
                        .header(AUTHORIZATION, auth.bearer(Role.ADMIN))
                        .param("activityId", "7"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        expectApiErrorShape(result, "INTERNAL_ERROR", "/api/admin/registrations/counts");
    }
}
