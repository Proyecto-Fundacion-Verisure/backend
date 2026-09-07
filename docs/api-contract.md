# Contrato de API · Voluntariado Fundación Verisure

**Borrador para la sesión de C-03 · 7 de septiembre de 2026**

> C-03 ([#179](https://github.com/Proyecto-Fundacion-Verisure/backend/pull/179)) y C-04 ([#180](https://github.com/Proyecto-Fundacion-Verisure/backend/pull/180)) ya están mergeados en `dev`.

> Este documento es la **única** fuente del contrato entre backend y frontend. Sustituye a la tabla de endpoints del issue [#117](https://github.com/Proyecto-Fundacion-Verisure/backend/issues/117) y a las citas por pantalla del documento de frontend, que estuvieron escritas dos veces durante dos semanas sin que nada comprobara que coincidían.
>
> **Si una pantalla necesita un campo que no está aquí, no se inventa:** se pide el cambio, se actualiza este documento y luego se implementa. Cualquier cambio posterior se hace **por PR**, nunca en un mensaje de chat.

**Estado:** borrador pendiente de repasar en voz alta con las tres personas de frontend. Las secciones marcadas con ⚠️ son las que hay que acordar en esa sesión, y siguen todas abiertas.

Lo que ha cambiado desde el 2 de septiembre no es el contrato, sino su coste: **el código de C-03 y C-04 ya está escrito sobre estas decisiones**. Los ocho enumerados, la forma de `ApiError`, los dieciocho códigos, las siete firmas que cruzan dominios, los trece avisos y la cadena de seguridad están implementados tal como se describen aquí. Cambiar cualquiera de los puntos ⚠️ en la sesión ya no es editar un documento: es un cambio de código.

---

## 1 · Convenciones globales

| | |
|---|---|
| Formato | JSON, nombres en **camelCase** siempre |
| Autenticación | `Authorization: Bearer <jwt>` |
| Caducidad del token | **2 horas** (`app.jwt.expiration-ms=7200000`). La caducidad **es** la política de revocación |
| Cierre de sesión | `POST /api/auth/logout` responde 204 y solo deja traza. **No revoca el JWT.** El cliente limpia la sesión en el acto |
| Paginación | `?page=&size=`, devuelve `Page<T>` de Spring Data |
| Errores | Una única forma, `ApiError`, para **todos** los casos, incluidos los de validación por campo |
| Fechas | ISO-8601. `LocalDate` para fechas de actividad, `Instant` para marcas de tiempo |
| Enlaces de correo | Se forman con `app.base-url` + la ruta que entrega FE2. **Ninguna plantilla lleva un dominio fijo** |

### Rutas que entrega frontend

Backend no las inventa: las usa para construir los enlaces de los correos.

| Ruta | Para qué |
|---|---|
| `/my-volunteering` | «Mis voluntariados» |
| `/activities/{id}` | Detalle de actividad |
| ⚠️ `/closures/{id}` | Detalle del cierre. **Era `/reports/{id}`**; el cambio hay que acordarlo con FE2 en la sesión |

---

## 2 · Los ocho enumerados

Viajan **literales** en el JSON. Frontend los mockea tal cual.

| Enumerado | Valores |
|---|---|
| `Role` | `ADMIN` · `EMPLOYEE` · `PARTNER` |
| `UserStatus` | `PENDING_VERIFICATION` · `PENDING_APPROVAL` · `ACTIVE` · `REJECTED` |
| `Organization` | `VERISURE_ES` · `VERISURE_GROUP` · **nullable** para el rol de entidad |
| `PartnerStatus` | `PENDING` · `ACTIVE` · `REJECTED` |
| `ActivityStatus` | `DRAFT` · `PENDING_APPROVAL` · `PUBLISHED` · `FULL` · `IN_PROGRESS` · `FINISHED` · `CANCELLED` |
| `RegistrationStatus` | `WAITLISTED` · `CONFIRMED` · `REJECTED` · `CANCELLED` · `PENDING_CLOSURE` · `CLOSED` |
| `ActivityClosureStatus` | `DRAFT` · `CLOSED` |
| `ProposalStatus` | `NEW` · `ACCEPTED` · `REJECTED` |

Además, **`Registration.accepted` es un booleano** y **no** forma parte de `RegistrationStatus`. Marca que la administradora aceptó la solicitud; si no había hueco, la persona sigue en cola con `accepted = true`.

### Dos avisos sobre los nombres

> **La entidad `Partner` y el rol `PARTNER` son cosas distintas que comparten nombre.** `Partner` es la entidad colaboradora —una fila con su CIF—; `PARTNER` es el rol de las personas que trabajan en ella. Las rutas de ese rol son `/api/org/**`.
>
> **`ActivityStatus.DRAFT` y `ActivityClosureStatus.DRAFT` no son lo mismo.** El primero es una actividad que se está escribiendo; el segundo, un cierre a medio rellenar.

### Quién escribe cada estado

Ninguna transición existe si no hay una tarea que la escriba. Esta tabla es la que destapó que nadie ponía una actividad en `FINISHED`, y de ahí salió `B3-17`.

| `ActivityStatus` | Quién lo escribe |
|---|---|
| `DRAFT` | La admin o la entidad, al guardar sin publicar |
| `PENDING_APPROVAL` | La entidad, al enviar a revisión · `B2-14` |
| `PUBLISHED` | La admin, al publicar o al aprobar · `B2-02` · `B2-15` |
| `FULL` | `SpotService`, al cubrirse la última plaza · `B3-02` |
| `IN_PROGRESS` | **Tarea programada**, al llegar `startDate` · `B3-17` |
| `FINISHED` | **Tarea programada**, al pasar `endDate` · `B3-17` |
| `CANCELLED` | La admin, al cancelar · `B2-05` |

| `RegistrationStatus` | Quién lo escribe |
|---|---|
| `WAITLISTED` | `SpotService`, al solicitar plaza · `B3-02` |
| `CONFIRMED` | La decisión de la admin, o `promoteFirstInQueue` · `B3-03` · `B3-05` |
| `REJECTED` | La decisión de la admin · `B3-03` |
| `CANCELLED` | La persona o la admin, y `cancelAllForActivity` · `B3-06` |
| `PENDING_CLOSURE` | **Tarea programada**, al terminar la actividad · `B3-17` |
| `CLOSED` | `closeAllForActivity`, que llama BE1 al finalizar el cierre · `B3-06` |

---

## 3 · Errores

### La forma, única para todos los casos

```java
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path,
        Map<String, List<String>> fields) {}
```

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La solicitud no es válida",
  "timestamp": "2026-09-02T10:14:03Z",
  "path": "/api/auth/register",
  "fields": {
    "cif":  ["debe tener 9 caracteres", "solo admite letras y números"],
    "name": ["no puede estar vacío"]
  }
}
```

`fields` es `null` salvo en `VALIDATION_ERROR`. Es un **mapa de listas** y no un mapa de cadenas porque un mismo campo puede incumplir dos validaciones a la vez —un CIF que falla `@Size` y `@Pattern`—, y con un solo mensaje por campo uno de los dos se pierde de forma no determinista.

No lleva el código HTTP: ya viaja en la respuesta.

### Los 18 códigos de dominio

| Código | HTTP | Cuándo |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | Bean Validation falló · trae `fields` |
| `DEADLINE_PASSED` | 400 | Pasó la fecha límite de inscripción |
| `ACTIVITY_NOT_FINISHED` | 400 | Intentas cerrar antes de que la actividad termine |
| `INVALID_DATE_RANGE` | 400 | Fecha de fin anterior a la de inicio |
| `NOT_OWNER` | 403 | Pides un recurso que no es tuyo |
| `ACCOUNT_NOT_VERIFIED` | 403 | Falta confirmar el correo |
| `ACCOUNT_PENDING_APPROVAL` | 403 | La Fundación aún no ha aprobado la cuenta |
| `ACCOUNT_REJECTED` | 403 | La cuenta fue rechazada |
| `ALREADY_REGISTERED` | 409 | Ya tienes una inscripción que no cancelaste tú |
| `REGISTRATION_NOT_CONFIRMED` | 409 | Cierras una inscripción que no estaba confirmada |
| `ACTIVITY_NOT_CLOSED` | 409 | Pides el certificado de una actividad sin cerrar |
| `CLOSURE_ALREADY_CLOSED` | 409 | Corriges tu cierre con la actividad ya cerrada |
| `ACTIVITY_FINISHED` | 409 | Editas una actividad ya finalizada |
| `ACTIVITY_NOT_EDITABLE` | 409 | Ya está enviada a revisión o publicada |
| `CIF_ALREADY_REGISTERED` | 409 | Ese correo ya tiene cuenta en esa entidad |
| `PROPOSAL_ALREADY_DECIDED` | 409 | La propuesta ya se aceptó o rechazó |
| `VERIFICATION_EXPIRED` | 410 | El enlace del correo caducó |
| `RATE_LIMIT_EXCEEDED` | 429 | Límite por IP en las rutas públicas · `C-06` |

Los genéricos de HTTP —`UNAUTHORIZED` 401, `FORBIDDEN` 403, `NOT_FOUND` 404, `MALFORMED_REQUEST` 400, `PAYLOAD_TOO_LARGE` 413, `UNSUPPORTED_MEDIA_TYPE` 415, `INTERNAL_ERROR` 500— **no son códigos de dominio**, pero devuelven el mismo `ApiError`.

> 🚫 **`ACTIVITY_FULL` no existe y no debe aparecer nunca.** Aceptar una inscripción no falla por aforo: con hueco confirma, sin hueco conserva la cola con `accepted = true`.
>
> 🚫 **`REPORT_ALREADY_SUBMITTED` tampoco.** El cierre de participación no tiene estados, así que no hay reenvío que rechazar. Estaba en un criterio de aceptación de #117 y queda enmendado.
>
> 🚫 Tampoco pueden aparecer `Enrollment`, `SeatService` ni `/api/enrollments`. Los nombres son `Registration` y `SpotService`.

---

## 4 · Las siete firmas que cruzan dominios

Existen desde el día 1 aunque devuelvan vacío: es lo que permite que las tres compilen contra ellas sin esperarse. **Ningún dominio escribe directamente transiciones de otro.**

### Cinco de lectura

| Firma | Dónde vive | La escribe | La usa |
|---|---|---|---|
| `long countByActivityIdAndStatus(Long, RegistrationStatus)` | `RegistrationRepository` | BE3 | BE1, para los confirmados de la pantalla de cierre |
| `List<Registration> findByActivityIdAndStatusOrderByQueuePosition(Long, RegistrationStatus)` | `RegistrationRepository` | BE3 | BE3 |
| `List<ClosedParticipationView> findClosedForDashboard(Integer year, String line)` | ⚠️ **`ParticipationClosureRepository`** | BE3 | BE1, agregados del dashboard |
| `Optional<SpotInfo> findSpotInfo(Long activityId)` | `ActivityRepository` | BE2 | BE3, cupo, confirmadas y fecha límite sin tocar `Activity` |
| `Optional<ParticipationClosure> findByRegistrationId(Long)` | `ParticipationClosureRepository` | BE1 | BE3, para saber si una inscripción ya tiene cierre antes de permitir la baja |

> ⚠️ **`findClosedForDashboard` no está donde dice el runbook.** El runbook la sitúa en `RegistrationRepository` devolviendo `List.of()`. Vive en `ParticipationClosureRepository` con una `@Query` real y verificada, por dos razones: como método derivado **tumbaba el arranque de Spring**, y las horas salen de `ParticipationClosure`, no de `Registration`. Además `ClosedParticipationView` es un `record`, así que necesita expresión de constructor.

### Dos de escritura — las únicas del proyecto

```java
public interface RegistrationLifecycleService {

    /** La llama BE2 al cancelar una actividad. Pasa a CANCELLED las inscripciones vivas. */
    int cancelAllForActivity(Long activityId);

    /** La llama BE1 al FINALIZAR el cierre. Pasa todas de PENDING_CLOSURE a CLOSED. */
    int closeAllForActivity(Long activityId);
}
```

Se invocan de forma **síncrona, dentro de la transacción que llama**.

> **`closeRegistration(registrationId)` ya no existe.** Se cierra por actividad, porque la Fundación cierra una vez y arrastra todas sus participaciones.

---

## 5 · Avisos por correo

No hay eventos de Spring. Toda la comunicación entre dominios pasa por `NotificationService`, para que quién avisa a quién se lea en el constructor.

**Son trece métodos**, uno por aviso:

| Método | Cuándo | Lo llama |
|---|---|---|
| `notifyRegistrationConfirmed` | Plaza confirmada | BE3 |
| `notifyRegistrationWaitlisted` | Solicitud recibida, en cola | BE3 |
| `notifyRegistrationRejected` | Inscripción rechazada, **sin motivo** | BE3 |
| `notifySpotReleased` | Ascenso desde la cola | BE3 |
| `notifyActivityCancelled` | Actividad cancelada | BE2 |
| `notifyActivityFinished` | «Cuéntanos cómo fue» | `B3-17` |
| `notifyActivityClosed` | Cerrada · lleva el enlace al certificado | BE1 |
| `notifyActivitySubmittedForReview` | Una entidad envía a revisión | BE2 |
| `notifyActivityApproved` | La Fundación aprueba | BE2 |
| `notifyActivityReturned` | La Fundación devuelve · lleva el `reviewNote` | BE2 |
| `notifyOrgAccountApproved` | Cuenta de entidad aprobada | BE1 |
| `notifyOrgAccountRejected` | Cuenta de entidad rechazada | BE1 |
| `notifyVerificationRequested` | Enlace de verificación · **el único que no puede fallar en silencio** | BE1 |

### La regla que no se ve en ninguna firma

**El aviso se llama SIEMPRE fuera de la transacción.** Orquesta quien llama al servicio transaccional: el controlador en los endpoints, y el método `@Scheduled` en las tareas programadas —que por eso **no llevan `@Transactional`**—.

```java
// El servicio hace su trabajo y NO avisa
@Transactional
public ActivityClosureResponse finalizeClosure(Long id) { ... }

// El controlador avisa cuando el servicio ha vuelto sin lanzar
var body = activityClosureService.finalizeClosure(id);
notificationService.notifyActivityClosed(id);
```

Como red de seguridad, `NotificationServiceImpl` difiere el envío a `afterCommit()` si detecta una transacción abierta, y el envío real lo hace `MailDispatcher`, un bean aparte con `@Async`. **Es una protección, no un permiso para saltarse la regla.**

> ⚠️ **Dos trampas de *proxy*, que no dan ningún error.** `@Transactional` y `@Async` viven en un *proxy* alrededor del bean: una llamada de un método a otro **de la misma clase** no pasa por él y la anotación **se ignora en silencio**. Por eso `MailDispatcher` es un bean separado, y por eso no vale partir un servicio en un método público sin `@Transactional` que llame a uno interno que sí la lleve.

**Un caso que la regla no cubre sola:** cuando el destinatario nace dentro de la transacción y su identificador no está en la ruta. Es lo que pasa con `notifySpotReleased`, que va a quien ascendió de la cola. El servicio tiene que devolver ese dato:

```java
public record CancelResult(RegistrationResponse body, Long promotedRegistrationId) {}
```

---

## 5 bis · Seguridad

### Las cinco rutas públicas · sin token

| Método | Ruta |
|---|---|
| POST | `/api/auth/login` |
| POST | `/api/auth/register` |
| GET | `/api/auth/verify?token=` |
| POST | `/api/auth/resend-verification` |
| POST | `/api/proposals` — formulario de la landing, sin cuenta |

**Todo lo demás pide token**, incluidos el catálogo de actividades y `/uploads/**`.

### El reparto por rol

| Prefijo | Quién |
|---|---|
| `/api/admin/**` · `/api/dashboard/**` | `ADMIN` |
| `/api/org/**` | `PARTNER` |
| `GET /api/activities`, `/api/activities/{id}` | `EMPLOYEE` · `ADMIN` |
| `/uploads/**` | cualquiera con token |
| El resto | cualquiera con token · quién puede lo decide el servicio |

**Tres rutas sirven a dos roles**, así que la cadena solo exige token y la propiedad la comprueba el servicio devolviendo `NOT_OWNER`: `PATCH /api/registrations/{id}/cancel`, `GET /api/closures/{id}` y `GET /api/closures/{id}/certificate`.

### El token

- `Authorization: Bearer <jwt>`, caducidad de **2 horas**.
- Lleva dentro el correo y el rol, así que el servidor no consulta la base de datos en cada petición. La contrapartida: un cambio de rol o un rechazo de cuenta **no tienen efecto hasta que el token caduca**.
- **401 es «no sé quién eres»; 403 es «sé quién eres y no puedes».** Los dos devuelven `ApiError`.

### Errores del login

| Situación | Respuesta |
|---|---|
| Contraseña incorrecta **o correo inexistente** | **401** `UNAUTHORIZED`, con el mismo mensaje en ambos casos |
| Cuenta sin verificar | **403** `ACCOUNT_NOT_VERIFIED` |
| Cuenta pendiente de aprobación | **403** `ACCOUNT_PENDING_APPROVAL` |
| Cuenta rechazada | **403** `ACCOUNT_REJECTED` |

El mensaje idéntico para credenciales incorrectas y correo inexistente es deliberado: decir «ese correo no está registrado» permitiría averiguar quién tiene cuenta probando direcciones. El estado de la cuenta solo se revela **después** de acertar la contraseña.

---

## 6 · Endpoints

52 endpoints en once bloques.

### 6.1 · Autenticación

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/auth/login` | público | `LoginRequest { email, password }` | 200 `AuthResponse` | 401 genérico · 403 `ACCOUNT_NOT_VERIFIED` / `ACCOUNT_PENDING_APPROVAL` / `ACCOUNT_REJECTED` |
| POST | `/api/auth/logout` | autenticado | — | **204** · solo traza | — |
| GET | `/api/auth/me` | autenticado | — | 200 `UserResponse` | 401 |
| POST | `/api/auth/register` | público | `RegisterOrgRequest` | **201** | 400 `VALIDATION_ERROR` · 409 `CIF_ALREADY_REGISTERED` · 429 |
| GET | `/api/auth/verify?token=` | público | token | 200 | 410 `VERIFICATION_EXPIRED` · 429 |
| POST | `/api/auth/resend-verification` | público | `{ email }` | **204** | 429 `RATE_LIMIT_EXCEEDED` |

```
AuthResponse { accessToken, tokenType: "Bearer", expiresIn: 7200, user: UserResponse }
UserResponse { id, name, email, role, department?, organization? }
```

`organization` es nullable: quien tiene rol de entidad no pertenece ni a Verisure España ni a Verisure Grupo.

### 6.2 · Actividades · catálogo

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| GET | `/api/activities` | EMPLOYEE · ADMIN | `line`, `mode`, `from`, `to`, `page`, `size` | 200 `Page<ActivityCardResponse>` | 403 |
| GET | `/api/activities/{id}` | EMPLOYEE · ADMIN | — | 200 `ActivityDetailResponse` | 403 · 404 |
| PATCH | `/api/admin/activities/{id}/cancel` | ADMIN | — | **204** | 403 · 404 |

`GET /api/activities/{id}` es visible solo en `PUBLISHED`, `FULL`, `IN_PROGRESS` y `FINISHED`. En cualquier otro estado devuelve 404, no 403: quien no debe verla no debe ni saber que existe.

⚠️ **El catálogo ya no es público.** Pide token de `EMPLOYEE` o `ADMIN`. Una entidad colaboradora recibe **403**: lo suyo lo ve en `/api/org/activities`. La landing pública no lo necesita, porque sus cifras y líneas de acción son contenido estático.

Tras cancelar, frontend vuelve a consultar actividad e inscripciones.

### 6.3 · Actividades · administración

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| GET | `/api/admin/activities` | ADMIN | `status`, `page` | 200 `Page<ActivityRow>` | 403 |
| POST | `/api/admin/activities` | ADMIN | `CreateActivityRequest` | **201** `ActivityResponse` | 400 `INVALID_DATE_RANGE` |
| GET | `/api/admin/activities/{id}` | ADMIN | — | 200 `ActivityFormResponse` | 403 · 404 |
| PUT | `/api/admin/activities/{id}` | ADMIN | `UpdateActivityRequest` | 200 `ActivityResponse` | 409 `ACTIVITY_FINISHED` |
| PATCH | `/api/admin/activities/{id}/publish` | ADMIN | — | 200 `ActivityResponse` | 409 `ACTIVITY_NOT_EDITABLE` |
| POST | `/api/admin/activity-images` | ADMIN | multipart, parte `image` | **201** `ImageUploadResponse { url }` | 400 · 403 · 413 · 415 |
| GET | `/api/admin/activities/pending` | ADMIN | `page` | 200 `Page<ActivityRow>` | 403 |
| PATCH | `/api/admin/activities/{id}/approve` | ADMIN | — | 200 `ActivityResponse` | 409 |
| PATCH | `/api/admin/activities/{id}/return` | ADMIN | `ReturnRequest { note }` | 200 `ActivityResponse` | 409 |

- `GET /api/admin/activities/{id}` admite **cualquier** estado, incluidos `DRAFT` y `CANCELLED`. Es la diferencia con el detalle del catálogo, que solo muestra los estados visibles.
- La portada acepta **JPG y PNG, máximo 5 MB**. `CreateActivityRequest.imageUrl` usa exactamente la URL que devuelve este endpoint.
- `approve` y `return` son para actividades **propuestas por una entidad**. Los cierres **no** se devuelven.

### 6.4 · Propuestas

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/proposals` | **público** | `CreateProposalRequest` | **201** | 400 · 429 `RATE_LIMIT_EXCEEDED` |
| GET | `/api/admin/proposals` | ADMIN | `status`, `page` | 200 `Page<ProposalRow>` | 403 |
| GET | `/api/admin/proposals/{id}` | ADMIN | — | 200 `ProposalDetailResponse` | 403 · 404 |
| POST | `/api/admin/proposals/{id}/accept` | ADMIN | — | **201** `ActivityResponse` | 409 `PROPOSAL_ALREADY_DECIDED` |
| PATCH | `/api/admin/proposals/{id}/reject` | ADMIN | — | **204** | 409 `PROPOSAL_ALREADY_DECIDED` |

Aceptar devuelve **201** y no 200 porque crea una actividad nueva.

### 6.5 · Inscripciones

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/registrations` | EMPLOYEE | `{ activityId }` | **201** `RegistrationResponse` | 400 `DEADLINE_PASSED` · 409 `ALREADY_REGISTERED` |
| GET | `/api/registrations/me` | EMPLOYEE | — | 200 `List<MyRegistrationItem>` | 401 |
| GET | `/api/admin/registrations` | ADMIN | `activityId`, `status`, `page` | 200 `Page<RegistrationRow>` | 403 |
| PATCH | `/api/registrations/{id}/accept` | ADMIN | — | 200 `RegistrationResponse` | 404 |
| PATCH | `/api/registrations/{id}/reject` | ADMIN | — **sin motivo** | 200 `RegistrationResponse` | 404 |
| PATCH | `/api/registrations/{id}/cancel` | EMPLOYEE **o** ADMIN | `CancelRequest { reason? }` | 200 `RegistrationResponse` | 403 `NOT_OWNER` · 404 |

```
MyRegistrationItem {
  registrationId,
  activity { id, title, partner, startDate, endDate, hours },
  status,
  queuePosition?,
  closureId?,
  activityClosed
}
```

⚠️ **`closureId` y `activityClosed` sustituyen a `reportId` y `reportStatus`.** Como el cierre de participación no tiene estados, el booleano es lo único que permite decidir qué botón pintar:

| `closureId` | `activityClosed` | Botón en «Mis voluntariados» |
|---|---|---|
| `null` | `false` | «Cerrar tu participación» |
| tiene valor | `false` | «Cierre enviado» · solo lectura |
| tiene valor | `true` | «Descargar certificado» |

- **Cancelar es un único endpoint para los dos roles.**
- Rechazar **no admite motivo**, por contrato.
- Tras cancelar, frontend vuelve a consultar tablero y cola para ver las promociones.

### 6.6 · Favoritos

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/favorites` | EMPLOYEE | `FavoriteRequest { activityId }` | **201** | 404 |
| DELETE | `/api/favorites/{activityId}` | EMPLOYEE | — | **204** | 404 |

Restricción única `(activity_id, user_id)`.

### 6.7 · Cierre de participación · lo rellena el empleado

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| POST | `/api/closures` | EMPLOYEE | multipart: parte `request` (JSON) + parte opcional `evidence` | **201** `ClosureDetailResponse` | 400 `ACTIVITY_NOT_FINISHED` · 409 `REGISTRATION_NOT_CONFIRMED` / `CLOSURE_ALREADY_CLOSED` · 413 · 415 |
| GET | `/api/closures/{id}` | ADMIN o propietaria | — | 200 `ClosureDetailResponse` | 403 `NOT_OWNER` · 404 |
| GET | `/api/closures/{id}/certificate` | propietaria | — | 200 `CertificateResponse` | 409 `ACTIVITY_NOT_CLOSED` · 403 `NOT_OWNER` |

```
CreateClosureRequest { registrationId, actualHours, rating (1..5), comment?, evidenceConsent }
```

- `registrationId` va **en el cuerpo**, no en la ruta.
- La evidencia acepta **PDF, JPG y PNG, máximo 10 MB**. Si hay archivo, `evidenceConsent` debe ser `true`.
- **Solo puede existir un cierre por inscripción.** El identificador de ruta es siempre `closureId`.
- ⚠️ **No hay estados ni horas validadas.** Las horas que declara el empleado son las definitivas: nadie las corrige. Si a administración no le cuadran, lo escribe en `closingNotes` del cierre de actividad. Por eso no existe ningún endpoint de validar ni de devolver un cierre.

### 6.8 · Cierre de actividad · lo rellena la Fundación

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| GET | `/api/admin/activities/pending-closure` | ADMIN | `page` | 200 `Page<ActivityClosureRow>` | 403 |
| GET | `/api/admin/activities/{id}/closure` | ADMIN | — | 200 `ActivityClosureResponse` · borrador + agregados | 403 · 404 |
| PUT | `/api/admin/activities/{id}/closure` | ADMIN | `SaveActivityClosureRequest` | 200 `ActivityClosureResponse` | 409 `CLOSURE_ALREADY_CLOSED` |
| PATCH | `/api/admin/activities/{id}/closure/finalize` | ADMIN | — | 200 `ActivityClosureResponse` | 409 `CLOSURE_ALREADY_CLOSED` |

```
SaveActivityClosureRequest { collaborationRating (1..5)?, closingNotes?, lessonsLearned? }
ActivityClosureResponse  { activityId, collaborationRating, closingNotes, lessonsLearned,
                           status, closedAt,
                           expectedHours, reportedHours,
                           confirmedVolunteers, closedParticipations, evidenceCount }
```

- ⚠️ **La bandeja lista actividades, no cierres individuales.** La administradora no revisa doce formularios: mira los totales —previsto frente a reportado— y cierra **una vez**, lo que arrastra todas las participaciones a `CLOSED`.
- **`finalize` no se deshace.**
- `expectedHours` es `activity.hours × confirmedVolunteers`; `reportedHours`, `closedParticipations` y `evidenceCount` salen de una proyección agregada sobre los cierres de participación.

### 6.9 · Rol de entidad · `/api/org/**`

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| GET | `/api/org/activities` | PARTNER | `status`, `page` | 200 `Page<OrgActivityRow>` | 403 |
| POST | `/api/org/activities` | PARTNER | `CreateActivityRequest` | **201** `OrgActivityRow` | 400 |
| PUT | `/api/org/activities/{id}` | PARTNER | `UpdateActivityRequest` | 200 `OrgActivityRow` | 409 `ACTIVITY_NOT_EDITABLE` · 403 `NOT_OWNER` |
| PATCH | `/api/org/activities/{id}/submit` | PARTNER | — | 200 `OrgActivityRow` | 409 `ACTIVITY_NOT_EDITABLE` |
| GET | `/api/org/proposals` | PARTNER | `page` | 200 `Page<OrgProposalRow>` | 403 |
| POST | `/api/org/proposals` | PARTNER | `CreateOrgProposalRequest` | **201** `OrgProposalRow` | 400 |
| GET | `/api/org/dashboard` | PARTNER | `year` | 200 `OrgDashboardResponse` | 403 |

> **Barrera de datos personales · `B1-19`.** Todas estas rutas resuelven el `partnerId` **desde la sesión, nunca desde un parámetro**. Y **ninguno** de sus DTO expone nombres, correos, departamentos ni horas individuales de empleados. Ni uno.

### 6.10 · Cuentas de entidad · administración

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| GET | `/api/admin/org-accounts` | ADMIN | `status`, `page` | 200 `Page<OrgAccountRow>` | 403 |
| PATCH | `/api/admin/org-accounts/{id}/approve` | ADMIN | — | 200 `OrgAccountRow` | 404 |
| PATCH | `/api/admin/org-accounts/{id}/reject` | ADMIN | — | 200 `OrgAccountRow` | 404 |

> **Rechazo con CIF existente.** Si el `Partner` ya estaba `ACTIVE` porque tiene otra cuenta aprobada, rechazar la nueva solicitud cambia **únicamente ese `User`** a `REJECTED`; el `Partner` y las demás cuentas conservan su estado. Solo pasa también el `Partner` a `REJECTED` cuando era nuevo, estaba en `PENDING` y no tiene ninguna cuenta activa.

### 6.11 · Dashboard de la Fundación

| Método | Ruta | Rol | Recibe | Devuelve | Errores |
|---|---|---|---|---|---|
| GET | `/api/dashboard` | ADMIN | `year`, `line` | 200 `DashboardResponse` | 403 |
| GET | `/api/dashboard/participations.csv` | ADMIN | `year`, `line` | 200 `text/csv` | 403 |
| GET | `/api/dashboard/partners.csv` | ADMIN | `year` | 200 `text/csv` | 403 |
| GET | `/api/dashboard/report.pdf` | ADMIN | `year` | 200 `application/pdf` | 403 |

- Se calcula sobre participaciones **cerradas** (`RegistrationStatus.CLOSED`).
- **Los agregados por `Organization` excluyen a los usuarios con rol de entidad**, o aparecería una categoría vacía en los gráficos.
- Las descargas necesitan `Content-Disposition` expuesto en CORS.

---

## 7 · Lo que hay que acordar en la sesión

Los seis siguen abiertos: la sesión no se ha hecho.

| # | Punto | Por qué |
|---|---|---|
| 1 | ⚠️ Ruta de frontend `/reports/{id}` → **`/closures/{id}`** | La entrega FE2. Backend solo forma el enlace con `app.base-url` + la ruta |
| 2 | ⚠️ `MyRegistrationItem`: `closureId` + `activityClosed` | Sustituyen a `reportId` + `reportStatus`. Repasar la tabla de tres filas de §6.5 |
| 3 | ⚠️ Forma de `ApiError` con `Map<String, List<String>>` | Es una desviación del runbook, que usaba `Map<String, String>` |
| 4 | ⚠️ No existe validar ni devolver un cierre de participación | Si alguna pantalla lo contemplaba, hay que rehacerla |
| 5 | ⚠️ La bandeja de administración lista **actividades**, no cierres | Cambia la pantalla `admin-close` |
| 6 | Campos concretos de los DTO marcados `TODO C-03` en el código | `CreateClosureRequest`, `ClosureDetailResponse`, `CertificateResponse`, `ActivityClosureRow` |

### Dónde está ya cada punto en el código

Sirve para ver, antes de la sesión, qué cuesta cambiar cada cosa.

| # | Estado en `dev` |
|---|---|
| 1 | Implementado a medias: `app.base-url` ya existe en `application.properties`. La ruta la sigue entregando FE2, así que el cambio no toca backend |
| 2 | Sin implementar: `MyRegistrationItem` todavía no existe como DTO. Es el punto más barato de cambiar |
| 3 | **Implementado.** `exception/ApiError.java` usa `Map<String, List<String>>`, y `GlobalExceptionHandler` lo rellena así |
| 4 | **Implementado por omisión.** `ParticipationClosureService` no tiene ningún método de validar ni de devolver, y no hay endpoint que lo exponga |
| 5 | **Implementado.** `ActivityClosureController` lista actividades en `GET /api/admin/activities/pending-closure`, no cierres |
| 6 | Pendiente y **el más urgente de los seis**: los `record` de `dto/closure/` y `dto/activityclosure/` están vacíos con `TODO C-03`. Frontend no puede mockear ninguna pantalla de cierre hasta que tengan campos |

## 8 · Enmiendas pendientes de publicar

Este contrato se aparta de las fuentes escritas en varios puntos. Las enmiendas están redactadas en `docs/c-03-plan.md` y **aún no se han publicado**.

Conviene ser consciente de lo que eso significa hoy: **el código mergeado ya se comporta según estas enmiendas**, así que las issues y el runbook de la tabla describen un sistema que ya no es el que hay en `dev`. Quien las lea sin este documento al lado se va a equivocar.

| Documento | Qué se enmienda |
|---|---|
| **#117** | 8 entidades y no 7 · sin `validatedHours` · sin `ReportStatus` · sin `closeRegistration` · `PENDING_CLOSURE` · rutas `/api/closures` · desaparece `REPORT_ALREADY_SUBMITTED` de los criterios de aceptación |
| **#154** (B3-09) | Ya no hay `@TransactionalEventListener` · el correo «cierre validado o devuelto» no puede existir · ruta `/closures/{id}` |
| **#171** (B3-13) | Ya no hay `@TransactionalEventListener` |
| **#156** · **#157** | El rol se llama `PARTNER` en el código, no `ORG` |
| **Runbook v5** | Nombre de este documento · 18 códigos y no 16 · 13 avisos y no 12 · métodos `notifyXxx` · `findClosedForDashboard` vive en `ParticipationClosureRepository` · identidad real del proyecto (paquete, Java 25, `java-jwt`) |
