# Plan de ejecución · C-03 · Contrato de código y contrato de API

> Issue: [#117](https://github.com/Proyecto-Fundacion-Verisure/backend/issues/117) · Rama: `c-03-contrato-de-código-y-contrato-de-api` · Dependencias: #118 (cerrada) y #116
>
> **Hecho cuando:** las tres compilamos el proyecto con el contrato dentro y el equipo de frontend tiene la lista de endpoints **escrita**, no de palabra.

---

## 0. Dónde estamos hoy (revisión del código actual)

Ya está en la rama:

| Paquete | Estado |
|---|---|
| `entity/` + `entity/enums/` | 8 entidades y 7 enumerados completos, con relaciones (C-02) |
| `repository/` | 8 repositorios `JpaRepository` |
| `repository/projection/` | `SpotInfo`, `ClosedParticipationView` |
| `service/` | `NotificationService(+Impl)`, `RegistrationLifecycleService(+Impl)`, `ActivityClosureService(+Impl)` |
| `dto/user/`, `dto/activityclosure/` | 4 *records* vacíos (esqueleto) |
| `config/`, `security/`, `seeder/`, `mapper/`, `exception/`, `controller/` | clases vacías (solo el paquete) |

Verificado en esta revisión:

- `./mvnw compile` → **OK**.
- Arranque del contexto de Spring → **FALLA**. Ver Lote 0.

> ⚠️ Los ficheros de `service/` y `dto/activityclosure/` estaban **sin commitear** en el working copy. Commitearlos es el primer paso: si no están en la rama, las otras dos no compilan lo mismo.

---

## Lote 0 · Desbloquear el arranque (30 min, antes que nada)

Sin esto, el criterio «las tres compilan el proyecto» se cumple pero nadie puede levantar la aplicación.

1. **`RegistrationRepository.findClosedForDashboard(Integer year, String line)`** no es un nombre derivable. Spring Data intenta resolver la propiedad `findClosedForDashboard` sobre `Registration` y aborta la creación del bean:

   ```
   BeanCreationException: Error creating bean with name 'registrationRepository'
   No property 'findClosedForDashboard' found for type 'Registration'
   ```

   Arreglo: anotarla con `@Query` (JPQL de constructor, porque `ClosedParticipationView` es un *record*, no una interfaz de proyección):

   ```java
   @Query("""
       select new com.verisure.backend.repository.projection.ClosedParticipationView(
           u.department, cast(u.organization as string), a.line, pc.actualHours, a.endDate)
       from ParticipationClosure pc
         join pc.registration r
         join r.user u
         join r.activity a
       where r.status = com.verisure.backend.entity.enums.RegistrationStatus.CLOSED
         and (:year is null or year(a.endDate) = :year)
         and (:line is null or a.line = :line)
       """)
   List<ClosedParticipationView> findClosedForDashboard(@Param("year") Integer year,
                                                        @Param("line") String line);
   ```

   *(La consulta arranca en `ParticipationClosure`, así que su sitio natural es `ParticipationClosureRepository`. Decidir dónde vive, pero anotarla ya.)*

2. **`ActivityRepository.findSpotInfo`** hoy es un `default` que devuelve `Optional.empty()`. Compila y arranca, pero es una trampa silenciosa. Sustituir por `@Query` real o dejar el `default` con un `// TODO B3-02` explícito para que nadie lo dé por implementado.

3. **Errata de columna:** en `Activity.java` y `User.java` la FK es `@JoinColumn(name = "parter_id")` — falta la `n` de *partner*. Con `ddl-auto=update` esa columna ya se crea mal en la base de datos de todas. Corregir a `partner_id` **hoy**, antes de que haya datos.

4. Añadir `app.base-url` a `application.properties` y a `.env.example` (el contrato exige que las plantillas de correo no lleven dominios fijos).

**Responsable:** quien tenga el dominio de inscripciones (BE3) para el punto 1; puntos 2–4, la que abra el PR de C-03.

---

## Lote 1 · Alinear enumerados y nombres (decisión de las tres, 45 min)

El código de C-02 se adelantó al contrato del issue en varios sitios. Hay que cerrar cada divergencia **antes** de enseñar la lista a frontend, porque los enumerados viajan en el JSON.

| Contrato #117 | Código actual | Propuesta | Por qué |
|---|---|---|---|
| `Role { ADMIN, EMPLOYEE, ORG }` | `Role { ADMIN, EMPLOYEE, PARTNER }` | **Renombrar a `ORG`** | Las rutas ya son `/api/org/**` y el glosario canónico reserva *partner* para la **entidad colaboradora**, no para el rol. Usar `PARTNER` para las dos cosas es justo la confusión que el glosario intenta evitar. |
| `RegistrationStatus.PENDING_REPORT` | `PENDING_CLOSURE` | **Decidir con frontend en la reunión** | Este valor sale literal en `MyRegistrationItem.status`. Frontend lo mockea; si no coincide, sus pantallas rompen el día que conectemos. |
| Entidad `Report`, `ReportStatus { SUBMITTED, VALIDATED, RETURNED }` | `ParticipationClosure` **sin campo `status`** | Mantener el nombre JPA `ParticipationClosure`, **añadirle `ReportStatus status`** y exponer la API como `/api/reports` con `reportId` | El contrato con frontend es intocable (`/api/reports`, `reportId`, `ReportSummary`, `ReportDetailResponse`). El nombre de la clase Java es interno. Pero **falta el campo `status`**, y sin él no se puede implementar «reenviar un `RETURNED` actualiza el existente». |
| Falta `validatedHours` | — | Añadir `Integer validatedHours` a `ParticipationClosure` | Lo exige `ValidateRequest`. El campo público es `validatedHours`, **nunca** `validated_hours`. |
| `ActivityClosure` (DRAFT/CLOSED) | existe | **No está en #117** | Es una entidad nueva que introdujimos en C-02. Hay que añadir sus 3 endpoints al contrato por PR sobre el issue (ver Lote 5). |

También hace falta crear el `enum ReportStatus` en `entity/enums/`.

> **Regla acordada:** a partir de hoy, cualquier cambio a estos nombres se hace **por PR sobre #117**, nunca en un mensaje de chat.

---

## Lote 2 · Las seis firmas que cruzan dominios *(punto 2 del encargo)*

Se crean **hoy**, con cuerpo vacío o `return 0`, para que las tres podamos compilar contra ellas desde el día 1. El contrato lo dice explícitamente: *«BE2 y BE1 la invocan de forma síncrona dentro de la transacción que llama; ningún dominio escribe directamente transiciones de otro»*.

| # | Firma | La publica | La llama | Para qué |
|---|---|---|---|---|
| 1 | `int cancelAllForActivity(Long activityId)` | BE3 · `RegistrationLifecycleService` | BE2 | Al cancelar una actividad (`PATCH /api/activities/{id}/cancel`) |
| 2 | `int closeAllForActivity(Long activityId)` | BE3 · `RegistrationLifecycleService` | BE1 | Al finalizar el `ActivityClosure` |
| 3 | `void closeRegistration(Long registrationId)` | BE3 · `RegistrationLifecycleService` | BE1 | Al validar un cierre individual → `CLOSED` |
| 4 | `Optional<SpotInfo> getSpotInfo(Long activityId)` | BE2 · `ActivityQueryService` | BE3 | Cupo, confirmadas y fecha límite, sin que BE3 toque `Activity` |
| 5 | `int markInProgress(LocalDate today)` y `int markFinished(LocalDate today)` | BE2 · `ActivityLifecycleService` | BE3 (tarea programada B3-17) | BE3 dispara el reloj, BE2 es dueño de la transición de `ActivityStatus` |
| 6 | `List<ClosedParticipationView> findClosedForDashboard(Integer year, String line)` | BE3 · `ParticipationQueryService` | BE1 | Agregados del dashboard sin que BE1 lea tablas de inscripciones |

⚠️ **Ojo con la firma 3:** el contrato de #117 dice `closeRegistration(Long registrationId)`; el código actual solo tiene `closeAllForActivity`. Hacen falta **las tres**, no dos.

Cada `*Impl` va anotado `@Service @RequiredArgsConstructor` y cada método con `@Transactional`.

> 🔧 Cambiar `jakarta.transaction.Transactional` por **`org.springframework.transaction.annotation.Transactional`** en `ActivityClosureServiceImpl` y `RegistrationLifecycleServiceImpl`. Solo la de Spring soporta `propagation`, `readOnly` y `rollbackFor`, y es la que encaja con el `afterCommit` que ya usa `NotificationServiceImpl`.

---

## Lote 3 · Avisos: servicios en vez de eventos *(punto 3, re-expresado)*

**Decisión tomada: no habrá paquete `event/` con siete *records* ni un oyente vacío.** En su lugar, `NotificationService` con un método por aviso.

Justificación, para dejarla escrita en el issue:

- El propio contrato ya reserva los eventos asíncronos **solo** para efectos secundarios como el correo, y prohíbe que un dominio escriba transiciones de otro. Las transiciones ya van por las firmas síncronas del Lote 2, así que al evento solo le quedaba el correo.
- `NotificationServiceImpl` **ya resuelve bien la parte asíncrona**: registra un `TransactionSynchronization.afterCommit()`, de modo que el correo solo sale si la transacción confirma, y captura la excepción para que un fallo de correo nunca tumbe la operación de negocio. Eso es exactamente lo que aportaba `@TransactionalEventListener(AFTER_COMMIT)`, con una clase menos y con la firma tipada.
- Para tres personas en una semana, un método tipado se descubre en el IDE; un *record* de evento suelto, no.

**Lo que queda por hacer en este lote:**

1. Los «siete avisos por correo» de H23 (Épica 7) están ya cubiertos por los **12 métodos** de `NotificationService`. Mapear en el issue qué método corresponde a cada uno de los siete avisos oficiales, para que B3-09 / B3-13 / B3-14 sepan qué plantilla escribir.
2. **Bug:** `notifyActivityReturned` en `NotificationServiceImpl` **no lleva `@Override`**. Añadírselo — compila igual, pero oculta futuras erratas de firma.
3. Quitar el `throws IllegalStateException` de `private void send(...)`: es una excepción *unchecked* declarada sin motivo y confunde.
4. Dejar `send(...)` con su `// TODO B3-09`, apuntando a `mailService.send(...)` y a `app.base-url` para construir los enlaces.

---

## Lote 4 · `ApiError`, `GlobalExceptionHandler` y códigos de dominio *(punto 4)*

### 4.1 Forma común de error

Un único cuerpo para **todos** los errores, incluidos los de validación por campo (criterio de aceptación de #117):

```java
package com.verisure.backend.exception;

public record ApiError(
        String code,                 // ALREADY_REGISTERED
        String message,              // legible, en español
        int status,                  // 409
        String path,                 // /api/registrations
        Instant timestamp,
        List<FieldError> errors      // null salvo en VALIDATION_ERROR
) {
    public record FieldError(String field, String message) {}
}
```

### 4.2 Piezas a crear en `exception/`

- `ErrorCode` — *enum* con `(String code, HttpStatus status)`, fuente única de la tabla de abajo.
- `ApiException extends RuntimeException` — lleva un `ErrorCode` y un mensaje opcional.
- `NotFoundException`, `ConflictException`, `ForbiddenException` — atajos sobre `ApiException`.
- `GlobalExceptionHandler` — anotar `@RestControllerAdvice` (hoy es una clase vacía) y manejar:

  | Excepción | Código | HTTP |
  |---|---|---|
  | `ApiException` | el suyo | el suyo |
  | `MethodArgumentNotValidException` | `VALIDATION_ERROR` | 400 |
  | `ConstraintViolationException` | `VALIDATION_ERROR` | 400 |
  | `HttpMessageNotReadableException` | `MALFORMED_REQUEST` | 400 |
  | `AuthenticationException` | `UNAUTHORIZED` | 401 |
  | `AccessDeniedException` | `FORBIDDEN` | 403 |
  | `MaxUploadSizeExceededException` | `PAYLOAD_TOO_LARGE` | 413 |
  | `HttpMediaTypeNotSupportedException` | `UNSUPPORTED_MEDIA_TYPE` | 415 |
  | `Exception` | `INTERNAL_ERROR` | 500 |

  El manejador genérico registra la traza con `log.error` pero **nunca** la devuelve (`server.error.include-stacktrace=never` ya está puesto ✅).

### 4.3 Lista inicial de códigos de dominio

Sacada literal de #117, más los transversales:

| Código | HTTP | Origen |
|---|---:|---|
| `ALREADY_REGISTERED` | 409 | Convenciones |
| `REGISTRATION_NOT_CONFIRMED` | 409 | Convenciones |
| `REPORT_ALREADY_SUBMITTED` | 409 | Convenciones |
| `PROPOSAL_ALREADY_DECIDED` | 409 | Contrato 26/08 |
| `CIF_ALREADY_REGISTERED` | 409 | Ampliación ORG |
| `ACTIVITY_NOT_EDITABLE` | 409 | Ampliación ORG |
| `ACCOUNT_NOT_VERIFIED` | 403 | Ampliación ORG |
| `ACCOUNT_PENDING_APPROVAL` | 403 | Ampliación ORG |
| `ACCOUNT_REJECTED` | 403 | Ampliación ORG |
| `VERIFICATION_EXPIRED` | 410 | Ampliación ORG |
| `VALIDATION_ERROR` | 400 | Transversal |
| `MALFORMED_REQUEST` | 400 | Transversal |
| `UNAUTHORIZED` | 401 | Transversal |
| `FORBIDDEN` | 403 | Transversal |
| `NOT_FOUND` | 404 | Transversal |
| `PAYLOAD_TOO_LARGE` | 413 | Transversal |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Transversal |
| `INTERNAL_ERROR` | 500 | Transversal |

> 🚫 **`ACTIVITY_FULL` no existe** y no debe aparecer en el *enum*. Aceptar una inscripción nunca falla por aforo: con hueco confirma, sin hueco conserva la cola con `accepted = true`.
>
> 🚫 Tampoco pueden aparecer `Enrollment`, `SeatService` ni `/api/enrollments` en ninguna parte del repositorio.

---

## Lote 5 · Contrato de API completo *(punto 5)*

Se acuerda **con las tres de frontend delante** y se versiona en `docs/api-contract.md`. Convenciones globales: JSON en **camelCase**, JWT `Bearer` con caducidad de 2 h, paginación `?page=&size=` devolviendo `Page<T>`, y todo error con la forma `ApiError` del Lote 4.

### Autenticación

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/auth/login` | público | `LoginRequest { email, password }` | 200 `AuthResponse { accessToken, tokenType:"Bearer", expiresIn:7200, user }` | 401 genérico |
| POST | `/api/auth/logout` | autenticado | — | 204 (solo deja traza, **no revoca** el JWT) | — |
| GET | `/api/auth/me` | autenticado | — | 200 `UserResponse { id, name, email, role, department?, organization }` | 401 |
| POST | `/api/auth/register` | público (límite por IP) | `RegisterOrgRequest` | 201 | 409 `CIF_ALREADY_REGISTERED` |
| GET | `/api/auth/verify?token=` | público (límite por IP) | token | 200 | 410 `VERIFICATION_EXPIRED` |
| POST | `/api/auth/resend-verification` | público (límite por IP y correo) | email | 204 | — |

### Actividades · catálogo público y empleado

| Método | Ruta | Rol | Devuelve | Errores |
|---|---|---|---|---|
| GET | `/api/activities` | público | `Page<ActivityCardResponse>` (filtros `line`, `mode`, `from`, `to`) | — |
| GET | `/api/activities/{id}` | público | `ActivityDetailResponse` — visible solo en `PUBLISHED`, `FULL`, `IN_PROGRESS`, `FINISHED` | 404 |
| PATCH | `/api/activities/{id}/cancel` | ADMIN | 204 — frontend recarga actividad **e** inscripciones | 404 |

### Actividades · administración

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| GET | `/api/admin/activities` | ADMIN | `status`, `page` | `Page<ActivityRow>` | 403 |
| GET | `/api/admin/activities/{id}` | ADMIN | — | `ActivityFormResponse` — **cualquier** estado, incluidos `DRAFT` y `CANCELLED` | 403/404 |
| POST | `/api/admin/activities` | ADMIN | `CreateActivityRequest` | 201 `ActivityResponse` | 400/403 |
| PUT | `/api/admin/activities/{id}` | ADMIN | `UpdateActivityRequest` | 200 `ActivityResponse` | 409 `ACTIVITY_NOT_EDITABLE` |
| PATCH | `/api/admin/activities/{id}/publish` | ADMIN | — | 200 `ActivityResponse` | 409 |
| POST | `/api/admin/activity-images` | ADMIN | multipart, parte `image` · JPG/PNG · máx. 5 MB | 201 `ImageUploadResponse { url }` | 400/403/413/415 |
| GET | `/api/admin/activities/pending` | ADMIN | `page` | `Page<ActivityRow>` | 403 |
| PATCH | `/api/admin/activities/{id}/approve` | ADMIN | — | 200 `ActivityResponse` | 409 |
| PATCH | `/api/admin/activities/{id}/return` | ADMIN | `ReturnRequest { note }` | 200 `ActivityResponse` | 409 |

`CreateActivityRequest.imageUrl` usa exactamente la URL devuelta por `/api/admin/activity-images`.

### Propuestas

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/proposals` | público | `CreateProposalRequest` | 201 | 400 |
| GET | `/api/proposals` | ADMIN | `status`, `page` | `Page<ProposalRow>` | 403 |
| GET | `/api/proposals/{id}` | ADMIN | — | `ProposalDetailResponse` | 403/404 |
| POST | `/api/proposals/{id}/accept` | ADMIN | — | **201** `ActivityResponse` | 409 `PROPOSAL_ALREADY_DECIDED` |
| PATCH | `/api/proposals/{id}/reject` | ADMIN | — | **204** | 409 `PROPOSAL_ALREADY_DECIDED` |

### Inscripciones

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/registrations` | EMPLOYEE | `{ activityId }` | 201 `RegistrationResponse` | 409 `ALREADY_REGISTERED` |
| GET | `/api/registrations/me` | EMPLOYEE | — | `List<MyRegistrationItem>` | 401 |
| GET | `/api/admin/registrations` | ADMIN | `activityId`, `status`, `page` | `Page<RegistrationRow>` (tablero) | 403 |
| PATCH | `/api/registrations/{id}/accept` | ADMIN | — | 200 `RegistrationResponse` | 404 |
| PATCH | `/api/registrations/{id}/reject` | ADMIN | — (**sin motivo**) | 200 `RegistrationResponse` | 404 |
| PATCH | `/api/registrations/{id}/cancel` | EMPLOYEE o ADMIN | `CancelRequest { reason? }` | 200 `RegistrationResponse` | 403/404 |

`MyRegistrationItem = { registrationId, activity { id, title, partner, startDate, endDate, hours }, status, queuePosition?, reportId?, reportStatus? }`.

Cancelar es **un único endpoint** para los dos roles. Tras cancelar, frontend recarga tablero/cola para ver las promociones.

### Favoritos

| Método | Ruta | Rol | Devuelve |
|---|---|---|---|
| POST | `/api/favorites` | EMPLOYEE | 201 (cuerpo `{ activityId }`) |
| DELETE | `/api/favorites/{activityId}` | EMPLOYEE | 204 |

> ❓ **A confirmar con frontend:** #117 solo dice «Favoritos: POST y DELETE». Esta es la forma propuesta.

### Cierres (`Report`)

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/reports` | EMPLOYEE | `multipart/form-data`: parte `request` (`application/json`, `CreateReportRequest`) + parte opcional `evidence` (PDF/JPG/PNG, máx. 10 MB) | **201** al crear · **200** al reenviar uno `RETURNED` | 409 `REPORT_ALREADY_SUBMITTED` · 409 `REGISTRATION_NOT_CONFIRMED` · 413 · 415 |
| GET | `/api/reports/pending` | ADMIN | `page` | `Page<ReportSummary>` (ligero) | 403 |
| GET | `/api/reports/{id}` | ADMIN o propietaria | — | `ReportDetailResponse` | 403/404 |
| PATCH | `/api/reports/{id}/validate` | ADMIN | `ValidateRequest { validatedHours }` | 200 `ReportDetailResponse` | 404/409 |
| PATCH | `/api/reports/{id}/return` | ADMIN | `ReturnRequest { note }` | 200 `ReportDetailResponse` | 404/409 |
| GET | `/api/reports/{id}/certificate` | propietaria | — | 200 `application/pdf` | 403/404 |

`CreateReportRequest = { registrationId: Long, actualHours: Integer, rating: Integer (1..5), comment: String?, evidenceConsent: boolean }`.

Si viaja archivo, `evidenceConsent` debe ser `true`. El identificador de la ruta es siempre `reportId`. Solo puede existir **un** informe por inscripción. `ReportSummary` ≠ `ReportDetailResponse`: el primero es la fila del listado, el segundo trae comentario, evidencia y horas validadas.

### Cierre de actividad (`ActivityClosure`) — **nuevo, añadir a #117 por PR**

| Método | Ruta | Rol | Recibe | Devuelve |
|---|---|---|---|---|
| GET | `/api/admin/activities/{id}/closure` | ADMIN | — | `ActivityClosureResponse` |
| PUT | `/api/admin/activities/{id}/closure` | ADMIN | `SaveActivityClosureRequest` | 200 `ActivityClosureResponse` (borrador) |
| POST | `/api/admin/activities/{id}/closure/finalize` | ADMIN | — | 200 `ActivityClosureResponse` (`CLOSED` + `closedAt`) |

`SaveActivityClosureRequest = { collaborationRating: Integer (1..5)?, closingNotes: String?, lessonsLearned: String? }`.

Finalizar llama, dentro de la misma transacción, a `closeAllForActivity` y luego a `notifyActivityClosed` — que ya sale en `afterCommit`.

### Rol de entidad social (`/api/org/**`)

| Método | Ruta | Recibe | Devuelve |
|---|---|---|---|
| GET | `/api/org/activities` | `status`, `page` | `Page<OrgActivityRow>` |
| POST | `/api/org/activities` | `CreateActivityRequest` | `OrgActivityRow` |
| PUT | `/api/org/activities/{id}` | `UpdateActivityRequest` | `OrgActivityRow` |
| PATCH | `/api/org/activities/{id}/submit` | — | `OrgActivityRow` |
| GET | `/api/org/proposals` | — | `Page<OrgProposalRow>` |
| POST | `/api/org/proposals` | `CreateOrgProposalRequest` | `OrgProposalRow` |
| GET | `/api/org/dashboard` | `year` | `OrgDashboardResponse` |

**Barrera de datos personales (B1-19):** todas las rutas `/api/org/**` resuelven el `partnerId` **desde la sesión, nunca desde un parámetro**, y sus DTO no exponen nombres, correos, departamentos ni horas individuales de empleados.

### Cuentas de entidad (administración)

| Método | Ruta | Rol | Recibe | Devuelve |
|---|---|---|---|---|
| GET | `/api/admin/org-accounts` | ADMIN | `status`, `page` | `Page<OrgAccountRow>` |
| PATCH | `/api/admin/org-accounts/{id}/approve` | ADMIN | — | `OrgAccountRow` |
| PATCH | `/api/admin/org-accounts/{id}/reject` | ADMIN | — | `OrgAccountRow` |

Rechazar con CIF ya existente: si el `Partner` estaba `ACTIVE` por otra cuenta aprobada, solo ese `User` pasa a `REJECTED`. El `Partner` pasa a `REJECTED` **únicamente** si era nuevo, estaba en `PENDING` y no tiene ninguna cuenta activa.

### Dashboard de la Fundación

| Método | Ruta | Rol | Devuelve |
|---|---|---|---|
| GET | `/api/dashboard` | ADMIN | `DashboardResponse` (filtros `year`, `line`) |
| GET | `/api/dashboard/participations.csv` | ADMIN | `text/csv` |
| GET | `/api/dashboard/partners.csv` | ADMIN | `text/csv` |
| GET | `/api/dashboard/report.pdf` | ADMIN | `application/pdf` |

Los agregados por `Organization` **excluyen** usuarios con rol de entidad.

### Rutas que entrega frontend

FE2 entrega a BE3 `/my-volunteering`, `/activities/{id}` y `/reports/{id}`. Los enlaces de los correos se forman con `app.base-url` + esa ruta; **ninguna plantilla lleva dominio fijo**.

---

## Lote 6 · Reparto de los ocho `@Order` de los DataSeeder *(punto 6)*

Orden canónico de #117, con dueña asignada para que **ninguna use el mismo número**:

| `@Order` | Seeder | Dueña | Depende de | Por qué en ese sitio |
|---:|---|---|---|---|
| 1 | `UserSeeder` | BE1 (B1-01) | — | Siembra **solo** `ADMIN` y `EMPLOYEE`, con `partner = null`. Por eso puede ir antes que `Partner`. |
| 2 | `PartnerSeeder` | BE2 (B2-12) | — | `Partner` no tiene FK salientes |
| 3 | `ActivitySeeder` | BE2 (B2-01) | 1, 2 | FK a `partner` y a `createdBy` |
| 4 | `ProposalSeeder` | BE2 (B2-06) | 2, 3 | FK a `partner` y `activity` (nullable) |
| 5 | `RegistrationSeeder` | BE3 (B3-01) | 1, 3 | FK a `activity`, `user`, `decidedBy` |
| 6 | `FavoriteSeeder` | BE3 (B3-07) | 1, 3 | FK a `activity` + `user`, con única `(activity_id, user_id)` |
| 7 | `ReportSeeder` | BE1 (B1-03) | 5 | `ParticipationClosure` tiene FK única a `registration` |
| 8 | `OrgUserSeeder` | BE3 (B3-15) | 2 | Usuarios con rol de entidad; necesita `Partner` sembrado, y por eso va **después** de `UserSeeder(1)` |

**El orden respeta las dependencias** precisamente porque los usuarios se siembran en dos tandas: `UserSeeder(1)` mete los que no dependen de `Partner`, y `OrgUserSeeder(8)` mete los que sí.

Reglas comunes para las tres (van en `seeder/`):

```java
@Component
@Order(N)
@RequiredArgsConstructor
@Profile("!prod")                       // nunca en producción
public class XxxSeeder implements CommandLineRunner {
    private final XxxRepository repo;

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;   // idempotente: no duplica al reiniciar
        ...
    }
}
```

- `DataSeeder.java` (hoy vacío) se **borra** o se convierte en clase de utilidades comunes; no puede quedar como noveno *runner* sin numerar.
- Elegir **uno** entre `CommandLineRunner` y `ApplicationRunner` y que las tres usen el mismo: `@Order` solo garantiza el orden dentro del mismo tipo.

---

## Orden de trabajo del día

| # | Qué | Quién |
|---:|---|---|
| 1 | Commitear los ficheros sueltos de `service/` y `dto/` | quien los tenga |
| 2 | **Lote 0** — arreglar el arranque y la errata `parter_id` | BE3 + una |
| 3 | **Lote 1** — reunión corta de las tres: cerrar `ORG`, `PENDING_REPORT` vs `PENDING_CLOSURE`, `ReportStatus` | las tres |
| 4 | **Reunión con las tres de frontend** — repasar el Lote 5 entero y cerrar los puntos marcados con ❓ | las seis |
| 5 | **Lote 5** — volcar lo acordado a `docs/api-contract.md` y commitear | una redacta, dos revisan |
| 6 | En paralelo: **Lote 2** (BE3), **Lote 4** (BE1), esqueletos del **Lote 6** (cada una el suyo) | repartido |
| 7 | **Lote 3** — anotar la decisión «servicios en vez de eventos» como comentario en #117 | quien abra el PR |
| 8 | `./mvnw clean verify` en las tres máquinas + `git pull` | las tres |

---

## Comprobación final (criterios de aceptación de #117)

- [ ] `./mvnw compile` pasa en las tres máquinas **y la aplicación arranca**
- [ ] `docs/api-contract.md` está commiteado y frontend puede hacer *mocks* sin preguntar nada de palabra
- [ ] `grep -rniE "enrollment|seatservice|/api/enrollments" src/ docs/api-contract.md` no devuelve nada
- [ ] Los seis enumerados y `Registration.accepted` están documentados
- [ ] `ApiError`, `REPORT_ALREADY_SUBMITTED` y los errores por campo comparten una única forma
- [ ] El contrato distingue `ReportSummary` de `ReportDetailResponse`
- [ ] `ACTIVITY_FULL` no aparece en ninguna parte
- [ ] Las tres firmas de `RegistrationLifecycleService` existen y compilan
- [ ] Los ocho `@Order` son distintos y respetan las dependencias
- [ ] La decisión «servicios en vez de eventos» está escrita en #117
