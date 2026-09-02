package com.verisure.backend.service;

public interface RegistrationLifecycleService {
    
    /** La llama BE2 al cancelar una actividad. Devuelve cuántas ha cancelado. */
    int cancelAllForActivity(Long activityId);

    /** La llama BE1 al finalizar el cierre de una actividad. Devuelve cuántas ha cerrado. */
    int closeAllForActivity(Long activityId);

}
