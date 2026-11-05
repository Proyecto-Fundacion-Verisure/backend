package com.verisure.backend.dto.proposal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Lo que envía una organización sin cuenta desde el formulario de la landing.
 *
 * <p>Sin cuenta no hay entidad de la que sacar el contacto, así que viaja aquí y
 * se guarda en la propia propuesta. {@code line} es opcional: quien propone puede
 * no saber en qué línea encaja, y entonces llega vacía.
 */
public record CreateProposalRequest(
        @NotBlank(message = "Indica el nombre de la organización.")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
        String organizationName,

        @NotBlank(message = "Introduce un CIF válido.")
        @Pattern(regexp = "^[A-Za-z]\\d{7}[A-Za-z0-9]$", message = "Introduce un CIF válido.")
        String cif,

        @NotBlank(message = "Indica una persona de contacto.")
        @Size(max = 120, message = "El contacto no puede superar los 120 caracteres.")
        String contactName,

        @NotBlank(message = "Introduce un correo válido.")
        @Email(message = "Introduce un correo válido.")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres.")
        String email,

        @NotBlank(message = "Indica un teléfono de contacto.")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres.")
        String phone,

        @Min(value = 1, message = "Debe ser un número entero mayor que cero.")
        Integer estimatedVolunteers,

        @Size(max = 60, message = "La línea no es válida.")
        String line,

        @NotBlank(message = "Describe la necesidad.")
        @Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres.")
        String description,

        @AssertTrue(message = "Debes aceptar la política de privacidad.")
        boolean consent) {
}
