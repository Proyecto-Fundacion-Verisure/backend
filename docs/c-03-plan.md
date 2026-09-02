# C-03 · Reconciliar el runbook v5 con el código y con #117

## Contexto

El plan anterior de C-03 se escribió a partir del cuerpo del issue [#117](https://github.com/Proyecto-Fundacion-Verisure/backend/issues/117). Al aparecer el **runbook v5 (1 de septiembre)** se ve que ese cuerpo está desfasado en puntos de fondo, y que **el código de C-02 ya sigue al runbook, no a #117**.

Consecuencia directa: el Lote 1 del plan anterior (crear `ReportStatus`, añadir `status` y `validatedHours` a `ParticipationClosure`) **haría exactamente lo contrario de lo que manda el runbook** y rompería el modelo que ya está en la rama.

Este documento no ejecuta nada. Lista las **19 divergencias** una a una con lo que dice cada fuente, lo que hay hoy en el código y una recomendación, para decidirlas antes de tocar nada.

### Qué dice cada fuente

| Fuente | Fecha | Estado |
|---|---|---|
| Runbook v5 | 1 sep | El más reciente. Dice de sí mismo que sustituye a las versiones anteriores |
| #117 (cuerpo original) | ~20 ago | Desfasado en los puntos de abajo |
| #117 (ampliaciones 25 y 26 ago) | 25–26 ago | Vigentes salvo donde el runbook las corrige |
| Títulos del backlog (#124, #177) | — | Coinciden con el runbook |
| Código en la rama (C-02) | 28 ago | Coincide con el runbook salvo en `Role` |

### Verificado en el código (no supuesto)

- **8 entidades**, incluida `ActivityClosure` ✅
- **8 enumerados**, incluidos `ActivityClosureStatus`, `UserStatus`, `PartnerStatus`. **No hay `ReportStatus`** ✅
- `ActivityStatus` (7 valores, con `PENDING_APPROVAL`) y `RegistrationStatus` (6, con `PENDING_CLOSURE`) **ya son los del runbook** ✅
- `ParticipationClosure` tiene solo `actualHours`, `rating`, `comment`, `evidenceUrl`, `submittedAt` + FK única ✅
- `RegistrationLifecycleService` tiene **exactamente dos** métodos ✅
- `NotificationService` tiene **12** métodos ✅
- `Role` = `{ADMIN, EMPLOYEE, PARTNER}` ❌ única divergencia real del modelo

---

## Bloque 1 · Divergencias donde el código ya está bien (no se toca nada)

Se resuelven **enmendando documentación**, no editando código.

| # | Punto | #117 dice | Runbook v5 dice | Código | Recomendación |
|---|---|---|---|---|---|
| 1 | Nombre del cierre del empleado | **7** entidades, con `Report` | **8** entidades: `Report` se parte en `ParticipationClosure` + `ActivityClosure` | las 8 ✅ | ✅ **DECIDIDO: runbook.** No es un renombrado: `ActivityClosure` no existe en #117. Enmendar #117 |
| 2 | Horas validadas | `ValidateRequest { validatedHours }` | «las horas del empleado son las definitivas, no hay `validated_hours`» | sin el campo ✅ | ✅ **DECIDIDO: runbook.** Desaparecen `ValidateRequest` y el endpoint de validar por cierre. Elimina medio Lote 1 anterior |
| 3 | Estados del cierre | `ReportStatus {SUBMITTED, VALIDATED, RETURNED}` | no existe; el que hay es `ActivityClosureStatus {DRAFT, CLOSED}` | `ActivityClosureStatus` ✅ | ✅ **DECIDIDO: runbook.** No crear `ReportStatus`. Cae `REPORT_ALREADY_SUBMITTED` → **hay que enmendar un criterio de aceptación de #117** |
| 4 | Tercera firma cruzada | BE3 publica `closeRegistration(registrationId)` | «*ojo, que esto cambió*»: ahora `closeAllForActivity` | dos métodos ✅ | ✅ **DECIDIDO: runbook.** No añadir la tercera. Se confirma también el nombre `RegistrationLifecycleService` |
| 5 | `ActivityStatus` / `RegistrationStatus` | 6 y 6 valores, con `PENDING_REPORT` | 7 y 6, con `PENDING_APPROVAL` y `PENDING_CLOSURE` | ya correctos ✅ | ✅ **DECIDIDO: runbook.** Nada que hacer en código |

> **Efecto conjunto: el Lote 1 del plan anterior desaparece entero**, y el Lote 2 pierde su firma nº 3. Eran las dos primeras cosas que íbamos a implementar.

### Estado del Bloque 1 · cerrado el 1 de septiembre

Los cinco puntos decididos a favor del runbook v5. **No se modifica ni un archivo del proyecto.** Lo único que produce este bloque es un comentario de enmienda en #117, cuyo texto queda redactado abajo a la espera de aprobación.

```markdown
## Enmienda del contrato · C-03 · 1 de septiembre de 2026

El runbook v5 (1/09) corrige cinco puntos del cuerpo de este issue. El código de C-02
y los títulos del backlog (#123, #124, #177) ya siguen al runbook.

1. **Ocho entidades, no siete.** `Report` ya no existe: se parte en `ParticipationClosure`
   (lo rellena el empleado, uno por inscripción) y `ActivityClosure` (lo rellena la
   Fundación, uno por actividad). Ninguna entidad se llama `Report`.
2. **No hay horas validadas.** Las que declara el empleado (`actualHours`) son las
   definitivas. Desaparecen `ValidateRequest { validatedHours }` y el endpoint de validar
   por cierre. Si a administración no le cuadran, lo escribe en `closingNotes`.
3. **El cierre del empleado no tiene estados.** No existe `ReportStatus`: la fila existe
   o no existe. En su lugar hay `ActivityClosureStatus { DRAFT, CLOSED }` para el cierre
   de actividad. Desaparecen el reenvío de un cierre `RETURNED` y `REPORT_ALREADY_SUBMITTED`.
4. **La frontera con BE3 son dos métodos, no tres.** `RegistrationLifecycleService` publica
   `cancelAllForActivity(activityId)` y `closeAllForActivity(activityId)`.
   `closeRegistration(registrationId)` queda eliminado: existía para cerrar una inscripción
   al validar su informe, y esa validación ya no existe.
5. **`RegistrationStatus.PENDING_CLOSURE`**, no `PENDING_REPORT`. `ActivityStatus` tiene
   siete valores, con `PENDING_APPROVAL`. Los enumerados son **ocho**, no seis.

### Criterios de aceptación afectados

- «`ApiError`, `REPORT_ALREADY_SUBMITTED` y errores por campo tienen una forma común»
  → ese código ya no existe; la casilla queda como `ApiError` y errores por campo.
- «El contrato distingue `ReportSummary` de `ReportDetailResponse`»
  → pasan a ser `ClosureDetailResponse` y `ActivityClosureRow`.
- «Los seis enumerados y `accepted` están documentados» → son **ocho** enumerados.
```

---

## Bloque 2 · Divergencias que sí obligan a escribir código

### 6 · El aviso sale dentro de la transacción — **defecto real, no diferencia de plan**

`service/ActivityClosureServiceImpl.java:21-25`:

```java
@Transactional
public ActivityClosureResponse finalizeClosure(Long activityId) {
    ...
    notificationService.notifyActivityClosed(activityId);   // ← dentro de la transacción
```

Es literalmente lo que el runbook llama «el error que más cuesta esta semana»: si algo falla después, la base de datos se deshace pero el correo ya salió. **Decidido: se arregla en C-03**, porque es el patrón que las tres van a copiar.

El arreglo tiene tres partes:

1. **Sacar la llamada del servicio.** El servicio hace su trabajo y devuelve; el controlador avisa después.
2. **`config/AsyncConfig.java`** con `@Configuration @EnableAsync`. **Hoy no existe**, y sin él `@Async` se ignora en silencio.
3. **La regla en el Javadoc de `NotificationService`**, que es el único sitio que nadie puede saltarse porque sale al autocompletar.

```java
// service/ActivityClosureServiceImpl.java
@Transactional
public ActivityClosureResponse finalizeClosure(Long id) {
    closure.setStatus(CLOSED);
    registrationLifecycleService.closeAllForActivity(id);
    return mapper.toResponse(closure);
}   // ← ya no avisa

// controller/ActivityClosureController.java
@PatchMapping("/activities/{id}/closure/finalize")
public ResponseEntity<ActivityClosureResponse> finalizeClosure(@PathVariable Long id) {
    var body = activityClosureService.finalizeClosure(id);   // ya confirmó
    notificationService.notifyActivityClosed(id);
    return ResponseEntity.ok(body);
}
```

> **No caer en el atajo que no funciona:** partirlo en dos métodos de la misma clase, el público sin `@Transactional` llamando a uno interno que sí la lleve. `@Transactional` vive en un *proxy*, y una llamada interna no pasa por él: la anotación se ignora **sin dar ningún error**. Queda escrito en el contrato aunque no se use.

**Los dos casos que «el controlador orquesta» no cubre**, y que hay que dejar escritos hoy porque aparecen en la semana 3:

- **Tareas programadas.** `activityFinished` lo dispara `B3-17`, donde no hay controlador. La regla se enuncia como **«orquesta quien llama al servicio transaccional»**: el controlador en los endpoints, el método `@Scheduled` en la tarea. Ese método **no lleva `@Transactional`**.
- **Destinatarios que nacen dentro de la transacción.** `spotReleased` va a la persona que ascendió de la cola, cuyo id **no está en la ruta**. El servicio tiene que devolverlo:
  ```java
  public record CancelResult(RegistrationResponse body, Long promotedRegistrationId) {}
  ```
  Es el precio real de haber quitado los eventos de Spring.

### 7 bis · El backlog todavía prescribe los eventos de Spring — **hallazgo nuevo**

Revisadas las **58 issues abiertas**, solo dos mencionan eventos, pero son las dos de correo y arrastran instrucciones que ya no se pueden cumplir:

| Issue | Frase | Problema |
|---|---|---|
| **#154** (B3-09) | «Escuchar eventos con `@TransactionalEventListener(AFTER_COMMIT)` y `@Async`» | Los eventos se eliminaron en el runbook v4. Quien coja la tarea implementará oyentes de eventos que nadie publica |
| **#154** (B3-09) | Correo nº 7: «**Cierre validado o devuelto**» | Imposible tras los puntos 2 y 3: ya no se valida ni se devuelve un cierre. Los avisos equivalentes son `activityClosed` (+ enlace al certificado) y `activityFinished` («cuéntanos cómo fue») |
| **#154** (B3-09) | Ruta `/reports/{id}` | Tras el punto 1 no hay informes. **Es una ruta de frontend, la entrega FE2** — no se puede renombrar por PR en backend |
| **#171** (B3-13) | «Sus oyentes con `@TransactionalEventListener(AFTER_COMMIT)` y `@Async`, como los siete anteriores» | Mismo problema que #154 |

> **Lo de `/reports/{id}` es lo único de todo el Bloque 1 que se sale del backend.** #117 dice «FE2 entrega `/my-volunteering`, `/activities/{id}` y `/reports/{id}`; backend forma los enlaces con `app.base-url` + esa ruta». Cambiarla a `/closures/{id}` obliga a hablar con FE2; dejarla como está significa que la URL del navegador dice «report» y la API dice «closure».

### 7 · Falta el aviso de verificación de correo

El runbook lo detecta: **12 métodos y 13 plantillas**. `B3-13` pide verificación, cuenta aprobada y cuenta rechazada; las dos últimas tienen método, la primera no. Y `B1-15` dice que es «el único que no puede fallar en silencio».

✅ **DECIDIDO: añadirlo ahora.** `void notifyVerificationRequested(Long userId)` en la interfaz más su `Impl`. Pasa a **13 métodos**. Lo llama BE1 desde `B1-15` (`/api/auth/verify` y `/api/auth/resend-verification`); la plantilla la escribe BE3 en `B3-13`.

✅ **DECIDIDO sobre la ruta de frontend:** `/reports/{id}` pasa a **`/closures/{id}`**, y se lleva a la reunión con FE2 antes de escribirlo en el contrato, porque esa ruta la entregan ellas.

### 8 · `exception/` está a un quinto

Hoy solo existe `GlobalExceptionHandler.java`, y vacío. El runbook pide **cinco ficheros**, en este orden:

```
exception/
├── ErrorCode.java              enum · los 16 códigos con su HttpStatus
├── ApiError.java               record · lo que viaja en el cuerpo
├── DomainException.java        RuntimeException que lleva un ErrorCode dentro
├── NotFoundException.java      la única que no está en la tabla
└── GlobalExceptionHandler.java @RestControllerAdvice
```

`DomainException` llevando el `ErrorCode` dentro es lo que permite **un solo** `@ExceptionHandler` para los dieciséis, en vez de dieciséis clases y dieciséis métodos.

### 9 · La lista de códigos y la forma de `ApiError`

| | Plan anterior (de #117) | Runbook v5 |
|---|---|---|
| Nº de códigos | 18 | **16** |
| `REPORT_ALREADY_SUBMITTED` | sí | **no existe** — sin estados no hay reenvío |
| `PROPOSAL_ALREADY_DECIDED` | sí | no está en la tabla |
| Genéricos HTTP (`MALFORMED_REQUEST`, `UNAUTHORIZED`, `NOT_FOUND`, `PAYLOAD_TOO_LARGE`, `UNSUPPORTED_MEDIA_TYPE`, `INTERNAL_ERROR`) | sí | no |
| Nuevos que yo no tenía | — | `ACTIVITY_NOT_FINISHED` `ACTIVITY_NOT_CLOSED` `CLOSURE_ALREADY_CLOSED` `NOT_OWNER` `ACTIVITY_FINISHED` `INVALID_DATE_RANGE` `DEADLINE_PASSED` |
| Forma de `ApiError` | `(code, message, status, path, timestamp, List<FieldError>)` | `(code, message, timestamp, path, Map<String,String> fields)` |

✅ **DECIDIDO · 9a — 18 códigos.** Los 16 del runbook, más:

- **`PROPOSAL_ALREADY_DECIDED` (409)** — lo exigen `POST /api/proposals/{id}/accept` y `PATCH /api/proposals/{id}/reject` en el contrato cerrado de #117, y **el runbook lo olvida**.
- **`RATE_LIMIT_EXCEEDED` (429)** — el runbook señala el hueco sin cerrarlo. Se declara ahora aunque el límite por IP se implemente en `C-06`.

Los genéricos HTTP (401, 404, 413, 415, 500) **no son códigos de dominio**: se manejan en `GlobalExceptionHandler`, que devuelve un `ApiError` igual que los demás. **`REPORT_ALREADY_SUBMITTED` desaparece** (punto 3), y con él una casilla de aceptación de #117.

✅ **DECIDIDO · 9b — `Map<String, List<String>>`.**

```java
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path,
        Map<String, List<String>> fields) {}
```

Mantiene el acceso por clave del runbook (`error.fields.cif`) sin perder mensajes cuando un campo incumple dos validaciones a la vez —el caso real es un CIF que falla `@Size` y `@Pattern`, donde un `Map<String,String>` descarta uno de los dos de forma no determinista—. Sin campo `status`: el código HTTP ya viaja en la respuesta. Es el formato de Django REST y Laravel, así que a frontend no le resultará ajeno. **Desviación del runbook que hay que comunicar a frontend**, porque `ApiError` sí viaja en el contrato.

### 10 · El cierre son dos servicios, y solo existe uno

El runbook lo decide explícitamente («**`ClosureService` no existe**») para que BE1 no empiece `B1-03` con una clase que habría que partir.

| | `ParticipationClosureService` | `ActivityClosureService` |
|---|---|---|
| Existe hoy | ❌ **falta** | ✅ |
| Métodos | `submit` · `getById` | `findPendingClosure` · `getByActivity` · `saveDraft` · `finalizeClosure` |
| Controlador | `/api/closures/**` | `/api/activities/{id}/closure**` |
| ¿Manda correo? | **No** | Solo `finalizeClosure` |
| Tarea | `B1-03` | `B1-04` |

Más **`CertificateService`** para `GET /api/closures/{id}/certificate`, que tampoco existe.

- **Recomendación:** crear las dos interfaces con sus `Impl` vacíos en C-03, igual que las firmas cruzadas.
- **`ParticipationClosureController` no debe inyectar `NotificationService`.** Si algún día hace falta, es señal de que se ha colado un aviso donde no toca.
- Los **agregados** de `getByActivity` (horas previstas vs reportadas, confirmados vs cerrados, evidencias) **no se resuelven llamando de un servicio al otro**: van como proyección en `ParticipationClosureRepository`:
  ```java
  public record ActivityClosureAggregates(
          Integer reportedHours, long closedParticipations, long evidenceCount) {}
  ```
  Los confirmados salen de `registrationRepository.countByActivityIdAndStatus(id, CONFIRMED)` — **este es el sitio donde BE1 usa esa firma cruzada** — y las horas previstas son `activity.hours × confirmados`, una multiplicación en el servicio.

### 11 · Piezas de C-01 que nunca se hicieron *(decidido: entran en C-03)*

`C-01` está dada por cerrada (#118) pero estas tres no están:

| Pieza | Estado | Por qué importa |
|---|---|---|
| **MapStruct en `pom.xml`** | **0 menciones** | Sin el orden Lombok → binding → MapStruct en `annotationProcessorPaths`, el *mapper* se genera **vacío**: compila y devuelve todos los campos a `null` en ejecución. Hay que ponerlo **antes** del primer mapper; ya existe `mapper/UserMapper.java` |
| **`spring.jpa.open-in-view=false`** | sin configurar → `true` | Con `true` las consultas perezosas funcionan por accidente y el N+1 se vuelve invisible |
| **`config/AsyncConfig`** | no existe | Ya cubierto en el punto 6 |

También faltan en `application.properties` las propiedades que el runbook da por puestas: `app.jwt.secret`, `app.jwt.expiration-ms` (7200000, hoy comentadas y con otro nombre), `app.cors.allowed-origin` y **`app.base-url`**.

> `app.base-url` se había aplazado a `B3-09` en el plan anterior. **Se recupera aquí**: el runbook lo pone en `C-01`, y sin él las plantillas de correo no tienen dónde mirar.

---

## Bloque 2 · cerrado · qué se implementa

Todas las decisiones tomadas. Estos son los archivos que toca, y **nada más**.

| Archivo | Acción |
|---|---|
| `config/AsyncConfig.java` | **Crear** · `@Configuration @EnableAsync`. Sin él `@Async` se ignora en silencio |
| `service/NotificationService.java` | **Modificar** · 13º método `notifyVerificationRequested(Long userId)` + la regla en el Javadoc de la interfaz |
| `service/NotificationServiceImpl.java` | **Modificar** · 13º método, `@Override` que falta en `notifyActivityReturned`, quitar `throws IllegalStateException`, y el patrón híbrido de abajo |
| `service/MailDispatcher.java` | **Crear** · el bean que lleva el `@Async` (ver la trampa) |
| `service/ActivityClosureServiceImpl.java` | **Modificar** · quitar la llamada al aviso, cambiar el import de `@Transactional` |
| `service/ActivityClosureService.java` | **Modificar** · `getClosure` → `getByActivity`, añadir `findPendingClosure` |
| `service/RegistrationLifecycleServiceImpl.java` | **Modificar** · import de `@Transactional` |
| `controller/ActivityClosureController.java` | **Crear** · orquesta: servicio primero, aviso después |
| `service/ParticipationClosureService.java` + `Impl` | **Crear** · `submit`, `getById` · B1-03 |
| `service/CertificateService.java` + `Impl` | **Crear** · B1-06 |
| `repository/projection/ActivityClosureAggregates.java` | **Crear** · `(reportedHours, closedParticipations, evidenceCount)` |
| `repository/ParticipationClosureRepository.java` | **Modificar** · la `@Query` de agregados |
| `exception/ErrorCode.java` | **Crear** · enum, 18 códigos con su `HttpStatus` |
| `exception/ApiError.java` | **Crear** · con `Map<String, List<String>> fields` |
| `exception/DomainException.java` | **Crear** · `RuntimeException` con un `ErrorCode` dentro |
| `exception/NotFoundException.java` | **Crear** |
| `exception/GlobalExceptionHandler.java` | **Modificar** · `@RestControllerAdvice`, un solo handler para los 18 |
| `pom.xml` | **Modificar** · MapStruct + `annotationProcessorPaths` en el orden Lombok → binding → MapStruct |
| `src/main/resources/application.properties` | **Modificar** · `open-in-view=false`, `app.base-url`, `app.jwt.*`, `app.cors.*` |

### El patrón híbrido del aviso, y la trampa que tiene

Decidido: se conserva el `afterCommit()` que ya tiene el código **y** el envío real pasa a ser asíncrono. Así el aviso es seguro aunque alguien se salte la convención, y nunca bloquea la respuesta.

> ⚠️ **`@Async` sufre exactamente el mismo problema de *proxy* que `@Transactional`.** Si `send(...)` llama a un `dispatch(...)` **de su propia clase**, la llamada no pasa por el *proxy* y **`@Async` se ignora en silencio**: el correo se envía en el hilo de la petición y vuelve a bloquear. Por eso el envío va en un bean aparte, `MailDispatcher`, inyectado en `NotificationServiceImpl`. Es el mismo error que el runbook describe para `@Transactional` en el paso 2 de `C-03`, aplicado a la otra anotación.

```java
// service/MailDispatcher.java  — bean aparte: el @Async solo funciona a través del proxy
@Component @Slf4j
public class MailDispatcher {
    @Async
    public void dispatch(String subject, String kind, Long id) {
        try {
            // TODO B3-09 · mailService.send(...) con su plantilla y app.base-url
            log.info("TODO B3-09 · correo «{}» para {} {}", subject, kind, id);
        } catch (Exception e) {
            log.warn("No se pudo enviar «{}» para {} {}: {}", subject, kind, id, e.getMessage());
        }
    }
}
```

### Los 18 códigos

`VALIDATION_ERROR` 400 · `DEADLINE_PASSED` 400 · `ACTIVITY_NOT_FINISHED` 400 · `INVALID_DATE_RANGE` 400 · `ALREADY_REGISTERED` 409 · `REGISTRATION_NOT_CONFIRMED` 409 · `ACTIVITY_NOT_CLOSED` 409 · `CLOSURE_ALREADY_CLOSED` 409 · `ACTIVITY_FINISHED` 409 · `ACTIVITY_NOT_EDITABLE` 409 · `CIF_ALREADY_REGISTERED` 409 · `PROPOSAL_ALREADY_DECIDED` 409 · `NOT_OWNER` 403 · `ACCOUNT_NOT_VERIFIED` 403 · `ACCOUNT_PENDING_APPROVAL` 403 · `ACCOUNT_REJECTED` 403 · `VERIFICATION_EXPIRED` 410 · `RATE_LIMIT_EXCEEDED` 429.

---

## Bloque 3 · Divergencias de nomenclatura y documentación

| # | Punto | Runbook v5 | Código / plan anterior | Recomendación |
|---|---|---|---|---|
| 12 | Rutas del cierre del empleado | `/api/closures`, `closureId`, `CreateClosureRequest`, `ClosureDetailResponse` | `/api/reports`, `reportId`, `CreateReportRequest` | ✅ **DECIDIDO: runbook.** Ya construido en el Bloque 2 |
| 13 | `MyRegistrationItem` | `closureId`, `activityClosed` | `reportId`, `reportStatus` | ✅ **DECIDIDO: runbook.** `activityClosed` **sustituye** a los estados eliminados en el punto 3: es lo único que distingue «cierre enviado» de «descargar certificado». Llevar la tabla de tres filas a la reunión con frontend |
| 14 | Cola de la admin | `GET /api/admin/activities/pending-closure` → `Page<ActivityClosureRow>` | `GET /api/reports/pending` → `Page<ReportSummary>` | ✅ **DECIDIDO: runbook.** No es un renombrado: lista **actividades** pendientes de cerrar, no informes pendientes de validar. Ya construido en el Bloque 2 |
| 15 | Métodos de `NotificationService` | `activityClosed(...)` | `notifyActivityClosed(...)` | ✅ **DECIDIDO: mantener el código.** No viaja en el contrato, así que frontend no se entera. El prefijo hace que un `grep notify` encuentre todos los avisos y distinga un aviso de una consulta. **Enmendar el runbook** |
| 16 | Seeder nº 7 | `ClosureSeeder` (BE1, los **dos** cierres) | `ReportSeeder` (una entidad) | ✅ **DECIDIDO: runbook, y se crean los ocho esqueletos en C-03**, con `@Order`, `@Profile("!prod")` y guarda de idempotencia. **Se borra `DataSeeder.java`**, que sería un noveno *runner* sin numerar. Ojo al cruce: `ClosureSeeder` es de BE1 pero necesita las inscripciones en `PENDING_CLOSURE` que siembra BE3, o **el dashboard sale vacío en la demo** |
| 17 | Documento de contrato | `03-contrato-api.md`, **53 endpoints**, «la lista ya está escrita» | no existe | ✅ **DECIDIDO: se redacta como borrador en `docs/api-contract.md`.** Comprobado que **no existe en ninguno de los tres repositorios** de la organización (`backend`, `frontend`, `documentacion-po`) y que la búsqueda de código en la organización da **0 resultados**. El runbook planifica la sesión del miércoles sobre un documento que no está versionado, que es justo lo que pide el criterio de aceptación de #117. **Enmendar el runbook** con el nombre nuevo |
| 18 | Identidad del proyecto | repo `verisure-volunteering-backend`, paquete `com.fundacionverisure.volunteering`, **Java 21**, jjwt `0.12.5`, `application.yml` con perfiles | repo `backend`, paquete `com.verisure.backend`, **Java 25**, `java-jwt` de Auth0, `application.properties` sin perfiles | ✅ **DECIDIDO: mantener el código y enmendar el runbook.** Renombrar el árbol de paquetes o bajar de versión de Java por un documento no compensa, y rompería cualquier rama abierta de las otras dos |

### 18 bis · Lo que falta de C-01 y sí tiene consecuencias — **hallazgo nuevo**

Aparte de lo cosmético, tres piezas obligatorias de `C-01` no están:

| Pieza | Estado | Decisión |
|---|---|---|
| **Java Mail Sender** | ❌ **0 menciones de `mail` en el `pom.xml`** | ✅ **Se añade ahora.** Sin ella `B3-09` no puede ni empezar. No obliga a configurar nada: `MailDispatcher` sigue con su traza hasta la semana 4 |
| **Perfil `test`** | ❌ no hay `src/test/resources/` | ⏭️ **Aplazado a C-04**, donde el runbook lo pide junto a la cadena de seguridad abierta con `@WithMockUser` |
| **`docker-compose.yml`** | ❌ no existe | Sin decidir. Queda anotado |

> ⚠️ **El riesgo del perfil `test`, para que no se olvide en C-04.** Hoy `application.properties` es la única configuración que existe, así que **las pruebas se conectan a la base de datos de desarrollo**, con `ddl-auto=update` activo. El `pom.xml` trae H2 con `scope test` pero **nadie lo usa**: falta justo el perfil que lo activaría. Hoy es inofensivo porque el único test solo levanta el contexto, pero `B1-11`, `B2-09` y `B3-10` van a escribir pruebas que insertan y borran filas.
>
> Al montarlo en C-04 hay que decidir entre **H2** (rápido, ya está la dependencia, pero no es PostgreSQL) y **Testcontainers** (fiel, pero exige Docker y tarda). No es teórico: `findClosedForDashboard` usa `cast(... as string)` y `year(...)`, y `ParticipationClosure.comment` usa `columnDefinition = "text"`, que es sintaxis de PostgreSQL.
| 19 | `Role` | `{ADMIN, EMPLOYEE, ORG}` | `{ADMIN, EMPLOYEE, PARTNER}` | ✅ **DECIDIDO: mantener `PARTNER`.** Son **cuatro** fuentes a enmendar: #117, #156 («`Role` gana `ORG`»), #157 (`@WithMockUser(roles="ORG")`) y el runbook. En las cuatro hay que dejar escrito que la **entidad `Partner`** y el **rol `PARTNER`** son cosas distintas que comparten nombre |

### 20 · Una desviación mía del Lote 0, que conviene dejar consciente

El runbook pone `findClosedForDashboard` en **`RegistrationRepository`**, devolviendo `List.of()` hasta la semana 3. En el Lote 0 la moví a **`ParticipationClosureRepository`** con una `@Query` real, porque la consulta arranca en `ParticipationClosure` (de donde salen las horas) y `ClosedParticipationView` es un `record`, que necesita expresión de constructor.

✅ **DECIDIDO: se queda donde está, y se documenta.** La `@Query` real ya está implementada y verificada contra PostgreSQL; la del runbook es un esbozo que devolvía `List.of()`. Se enmienda el runbook y se deja escrito junto a las firmas cruzadas del contrato, porque **BE1 la va a buscar en `RegistrationRepository` y no la encontrará**.

---

## Bloque 3 · cerrado · qué se implementa

| Trabajo | Detalle |
|---|---|
| `pom.xml` | Añadir `spring-boot-starter-mail`, que falta desde C-01 |
| `seeder/` | Las **ocho** clases esqueleto con su `@Order`, `@Profile("!prod")` y guarda de idempotencia. **Borrar `DataSeeder.java`** |
| `docs/api-contract.md` | Borrador completo: convenciones, 8 enumerados, 18 códigos, 13 avisos, 7 firmas cruzadas y los 53 endpoints |
| Enmiendas (sin publicar) | Textos redactados para **#117**, **#154**, **#171**, **#156**, **#157** y el runbook |

**Nada de código nuevo salvo los seeders**: los renombrados de los puntos 12 a 14 ya se hicieron en el Bloque 2.

---

## Orden de trabajo propuesto

1. **Decidir los 20 puntos** de arriba. Nada se toca hasta entonces.
2. **Enmiendas de documentación** (puntos 1–5, 15, 18, 19): comentario en #117, #156, #157 y en el runbook. Sin código.
3. **Arreglo del aviso** (punto 6) + `AsyncConfig` + Javadoc de la regla + `notifyVerificationRequested` (punto 7).
4. **`exception/` completo** (puntos 8 y 9): los cinco ficheros, 16 códigos + `RATE_LIMIT_EXCEEDED`.
5. **Los dos servicios de cierre** (punto 10): interfaces y `Impl` vacíos, más la proyección de agregados.
6. **Piezas de C-01** (punto 11): MapStruct, `open-in-view`, propiedades `app.*`.
7. **Ocho seeders** con su `@Order`, y borrar `DataSeeder.java`, que hoy está vacío y sería un noveno *runner* sin numerar.
8. **`03-contrato-api.md`** con los 53 endpoints, **repasado con las tres de frontend delante** antes de mergear.

## Verificación

```bash
./mvnw clean verify
./mvnw test -Dtest=BackendApplicationTests     # el contexto tiene que levantar

# Vocabulario prohibido: no debe devolver nada
grep -rniE "enrollment|seatservice|/api/enrollments|ACTIVITY_FULL|ReportStatus|validated_hours" src/ docs/

# Ningún aviso dentro de un método transaccional
grep -rn -B5 "notificationService\." src/main/java --include=*ServiceImpl.java | grep -i transactional

# @EnableAsync existe, o @Async no hace nada
grep -rn "EnableAsync" src/main/java

# Los ocho @Order son distintos
grep -rhn "@Order" src/main/java/com/verisure/backend/seeder/ | sort
```

**Casillas de #117 que hay que enmendar por PR** antes de poder cerrarlo:

- «`ApiError`, **`REPORT_ALREADY_SUBMITTED`** y los errores por campo comparten una única forma» → ese código ya no existe.
- «El contrato distingue `ReportSummary` de `ReportDetailResponse`» → ahora son `ClosureDetailResponse` y `ActivityClosureRow`.
- «Los **seis** enumerados están documentados» → son **ocho**.
