package com.verisure.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda archivos subidos y devuelve la URL desde la que se sirven.
 *
 * <p>Es de BE2 y la consume la evidencia de los cierres (B1-03): el llamador
 * decide la subcarpeta ({@code evidencias}) y valida tipo y tamaño antes de
 * llamar. Este servicio no valida: solo genera el nombre en el servidor y
 * guarda.
 *
 * <p>Los errores de tipo se traducen en 415 en
 * {@link com.verisure.backend.exception.GlobalExceptionHandler}.
 */
public interface FileStorageService {

    /**
     * Guarda {@code file}, generando un nombre de archivo en el servidor
     * —nunca el que envía el cliente— y devolviendo la URL relativa a la raíz.
     *
     * @param file      el archivo multipart recibido
     * @param subfolder la subcarpeta dentro de {@code uploads/} (p. ej. {@code evidencias})
     * @return {@code /uploads/<subfolder>/<uuid>.<ext>}
     */
    String store(MultipartFile file, String subfolder);
}