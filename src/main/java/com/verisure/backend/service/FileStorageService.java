package com.verisure.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda archivos subidos y devuelve la URL desde la que se sirven.
 *
 * <p>Es de BE2: la usa {@code POST /api/admin/activity-images} (B2-03) para las
 * portadas y también la evidencia de los cierres (B1-03), con distinta subcarpeta.
 *
 * <p>La validación de tipo y tamaño se hace aquí, porque es la única puerta por
 * la que entran archivos al servidor. Los errores los traduce
 * {@link com.verisure.backend.exception.GlobalExceptionHandler} en el cuerpo
 * único {@code ApiError}: 415 para tipo y 413 para tamaño.
 */
public interface FileStorageService {

    /**
     * Valida y guarda {@code file}, generando un nombre de archivo en el servidor
     * —nunca el que envía el cliente— y devolviendo la URL relativa a la raíz.
     *
     * @param file      el archivo multipart recibido
     * @param subfolder la subcarpeta dentro de {@code uploads/} (p. ej. {@code portadas})
     * @return {@code /uploads/<subfolder>/<uuid>.<ext>}
     */
    String store(MultipartFile file, String subfolder);
}