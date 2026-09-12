# Backend · tareas y reparto · MVP Fundación Verisure

> Las tareas de backend del MVP repartidas entre tres personas en porciones verticales. Cada persona es dueña de un dominio completo, de la tabla al endpoint, y el reparto está construido para que nadie espere a nadie.

| | |
|---|---|
| **Equipo** | FemCoders Barcelona P9 · 3 personas de backend |
| **Duración** | 4 semanas · 20 jornadas |
| **Versión** | 7 de septiembre de 2026 · **v3** |

> **Qué cambia respecto de la v2 (23 de agosto).** La v2 se escribió **antes** de la sesión de C-03, y el contrato que salió de allí —`docs/api-contract.md`— cambió cosas que la v2 daba por buenas: el rol se llama `PARTNER` y no `ORG`, los avisos son trece métodos `notifyXxx` y no doce eventos de Spring, `findClosedForDashboard` cambió de dueña, y la lista de endpoints es otra. Esta v3 pone el documento al día con **lo que hay mergeado en `dev`**, no con lo que se planeó en agosto.
>
> **La lista de endpoints ya no vive aquí.** La §7 de la v2 tenía una tabla de rutas que se había quedado a medio camino entre lo acordado y lo implementado. Se ha eliminado: el contrato es `docs/api-contract.md` y no hay segunda copia. Mantener dos listas es exactamente el problema que C-03 vino a resolver.

---

## 0 · Estado real del proyecto · 7 de septiembre

Antes de nada, dónde estamos. Cada tarea de este documento lleva su estado en la cabecera, con esta leyenda:

| | |
|---|---|
| ✅ | Terminada y mergeada en `dev` |
| 🟡 | Empezada, le falta una parte identificada |
| ⬜ | Sin empezar |

**Lo que hay en `dev`:** las cuatro tareas conjuntas de arranque (`C-01` a `C-04`), las ocho entidades con sus relaciones, los ocho enumerados, `ApiError` con su `GlobalExceptionHandler`, los dieciocho códigos de error, las siete firmas que cruzan dominios, la interfaz `NotificationService` con sus trece métodos, la cadena de seguridad completa con JWT y CORS, y los **siete** seeders.

**Lo que no hay:** ningún controlador salvo `ActivityClosureController`, y sus servicios son andamios que devuelven `null` o `0`. `UserController` está vacío. El login funciona —lo sirve el filtro `JWTAuthentication`, no un controlador—, pero `logout` y `me` no existen. Y los `record` de `dto/closure/` y `dto/activityclosure/` **están vacíos**, con un `TODO C-03`: eso bloquea a frontend, que no puede mockear ninguna pantalla de cierre.

**Sobre el calendario.** Este documento planificaba cuatro semanas desde el 23 de agosto. La semana 1 ya ha pasado y las cifras de la sección 1 no se han vuelto a calcular: **re-fechar el reparto por semanas es una decisión de equipo que este documento no toma por su cuenta.** Lo que sí está actualizado es qué está hecho y qué no.

---

## 1 · La cuenta: 20 jornadas, 3 personas

Primero la unidad, porque se presta a confusión. Un **día-persona** es **una persona trabajando una jornada**. No es un día de calendario: si las tres trabajáis el mismo día, ese día del calendario consume tres días-persona.

| | | |
|---|---|---|
| **Jornadas de calendario** | **20** | Cuatro semanas de lunes a viernes. La defensa entra dentro. |
| **Horas por jornada** | **8, con picos de 9** | Lo que habéis dicho que podéis sostener. |
| **Jornadas efectivas por persona** | **17** | De cada jornada se va una parte en el punto de la mañana, en revisar código, en integrar y en atascos. Contar 20 de 20 es planificar para un equipo que no existe. 17 de 20 es un 85%, que ya es exigente. |
| **Capacidad total de backend** | **51 días-persona** | 17 × 3 personas. |
| **En trabajo conjunto** | **9,75** | Las tareas `C-01` a `C-06`. Ver la sección 3. |
| **En dominio propio** | **27 + 9,5** | Lo que cada una construye sola. La segunda cifra es el rol de entidad. |
| **En pruebas** | **7** | Bloque final priorizado, semana 4. Ver la sección 9. |
| **En documentación, revisión y demo** | **7,25** | README, Postman, OpenAPI, revisión de los PR de las otras dos, corrección y ensayo. |

> **Estas cuentas no cuadran, y el documento no lo esconde.** Con el rol de entidad incorporado, las cargas son **23,5 en BE1, 22,5 en BE2 y 20,5 en BE3** sobre 17 jornadas efectivas. **No cabe.** La capacidad se gestiona aparte; lo que toca aquí es que el número esté delante cuando haya que decidir.
>
> **La casilla peor es BE2 en la semana 3:** 8,75 jornadas sobre unas 4,25 efectivas, porque el ciclo de actividades del rol de entidad (`B2-13` a `B2-16`) cae encima del de propuestas (`B2-05` a `B2-07`). Mover `B2-14` y `B2-16` a la semana 4 le quita una jornada.
>
> **Dos cosas han cambiado a favor desde la v2.** `C-05` se resolvió dentro de `C-02` —`Partner` nació directamente como entidad, no hubo que reabrir el modelo— y `B3-15` está prácticamente hecha dentro de los seeders existentes. Son unas 1,25 jornadas que la tabla de arriba todavía cuenta.

> **Qué se ha caído para que quepa.** Por este orden: el **registro de auditoría** de las decisiones del admin, la **optimización de consultas** como tarea propia, y el tiempo de **revisión de código**. Además el **PDF con gráficos** del dashboard (`B1-10`) queda como la primera pieza a sacrificar: es la única de las tres exportaciones que no rompe ningún recorrido si falta.

---

## 2 · Cómo se reparte

Cada persona se lleva una **porción vertical**, no una capa: sus tablas, sus entidades, sus repositorios, sus servicios, sus controladores, sus DTO y sus tests. Nadie hace «todas las entidades» ni «todos los controladores», porque eso obliga a que las demás esperen.

| | Dominio | De qué es dueña | Historias |
|---|---|---|---|
| **BE1** | Acceso, cierres e informes | `User` · `Partner` · `ParticipationClosure` · `ActivityClosure` · `AuthService` · `OrgAccountService` · `ParticipationClosureService` · `ActivityClosureService` · `CertificateService` · `DashboardService` · `CsvExportService` · `PdfExportService` | H1 · H2 · H16 · H17 · H18 · H19 · H20 · H21 · H22 · H25 · H26 · H30 |
| **BE2** | Catálogo: actividades y propuestas | `Activity` · `Proposal` · `ActivityService` · `ProposalService` · `FileStorageService` | H4 · H5 · H6 · H7 · H10 · H11 · H27 · H28 · H29 · H31 |
| **BE3** | Participación: inscripciones, cupo, cola y correo | `Registration` · `Favorite` · `SpotService` · `RegistrationService` · `RegistrationLifecycleService` · `FavoriteService` · `NotificationService` | H8 · H9 · H12 · H13 · H14 · H15 · H23 · H24 |

> **Ojo con dos nombres que la v2 tenía mal.** No hay un `ClosureService`: son **dos** servicios distintos, `ParticipationClosureService` (el cierre del empleado) y `ActivityClosureService` (el de la Fundación), y así están en el código. Y no hay `MailService`: el envío lo hace `NotificationService` con `MailDispatcher` detrás.

| Persona | Semana 1 | Semana 2 | Semana 3 | Semana 4 | Total |
|---|---|---|---|---|---|
| **BE1** | 6,5 | 6 | 5,5 | 5,5 | **23,5** |
| **BE2** | 5,75 | 3,5 | 8,75 | 4,5 | **22,5** |
| **BE3** | 5,5 | 4 | 5 | 6 | **20,5** |

---

## 3 · Las seis tareas conjuntas

Las tres personas juntas, sin repartirse. Son **4,25 días-persona de cada una**, 12,75 de equipo. Es caro, y es exactamente lo que permite que todo lo demás sea trabajo en paralelo de verdad.

### `C-01` Arranque del proyecto y estructura de paquetes ✅ · 0,5 d por persona

Spring Initializr con Web, JPA, Security, Validation, Mail, Lombok y el driver de PostgreSQL. Paquetes por capa: `controller`, `service`, `repository` (con `repository/projection`), `entity` (con `entity/enums`), `dto` agrupado por dominio, `mapper`, `exception`, `config`, `security` y `seeder`. **No hay paquete `event`:** los avisos van por `NotificationService`, dentro de `service`.

**Lo que quedó en el código, para quien llegue nuevo:** el proyecto es **Java 25**, el paquete raíz es `com.verisure.backend`, los JWT los firma **`java-jwt`** de Auth0 (no `jjwt`), y las variables de entorno las carga `spring-dotenv` desde un `.env` que no está en el repositorio. La configuración es un único `application.properties`, no un `application.yml` por perfil: los perfiles se distinguen por variables de entorno y por `@Profile("!prod")` en los seeders.

> **Hecho cuando:** Las tres clonan, ejecutan `mvn spring-boot:run` y arranca contra su Postgres local. Ninguna ve _getters_ en rojo en su editor.

### `C-02` Modelo de datos: entidades JPA y relaciones ✅ · 1 d por persona

Como no hay Flyway, las entidades son el esquema: lo que se escribe aquí es literalmente lo que Hibernate crea en Postgres.

**Las ocho entidades:** `Partner`, `User`, `Activity`, `Registration`, `ParticipationClosure`, `ActivityClosure`, `Favorite` y `Proposal`. Cada una con sus `@Column` (nombre, tipo, `nullable`, `length`), sus `@Enumerated(EnumType.STRING)` y sus `@ManyToOne`.

**Hay dos cierres, y son cosas distintas.** `ParticipationClosure` es **lo que rellena cada empleado** al terminar: horas, valoración y evidencia, uno por inscripción. `ActivityClosure` es **lo que cierra la Fundación**: calificación de la colaboración, notas y lecciones aprendidas, uno por actividad. La administradora no revisa doce formularios: mira los totales y cierra la actividad una vez.

**Y ninguno se llama `Report`.** En este proyecto «informe» significa el dashboard y los descargables, que no son tablas sino consultas. Para el formulario se dice **cierre**.

**`Partner` es una entidad, no un texto.** Tiene tabla propia con `cif` **único**: hay que poder engancharle un usuario y saber qué actividades son suyas. Comparar cadenas no vale — un «Caritas» sin tilde parte las horas de una entidad en dos y el dashboard miente sin dar ningún error.

**Ojo con un nombre que se repite:** «organización» significa dos cosas. En `User` es **a qué parte de Verisure pertenece la persona** — el enumerado `Organization` {`VERISURE_ES`, `VERISURE_GROUP`} — y es lo que antes llamábamos «sede». En `Activity` es **la entidad social con la que se colabora**, que en código se llama `partner` precisamente para que no se confundan.

**Las dos restricciones de unicidad que sí se declaran:** `users.email` y `favorites (activity_id, user_id)`. Más las de uno a uno: `activity_closures.activity_id` y `participation_closures.registration_id`, y `partners.cif`.

**Sobre la inscripción repetida:** la regla es «_una persona no puede volver a inscribirse en una actividad, salvo que la inscripción anterior la cancelara ella misma_». Eso **no es una restricción de unicidad** — puede haber varias filas de la misma persona y la misma actividad si canceló y volvió — así que **no lleva constraint en la tabla**: la comprueba `SpotService` antes de insertar, con `existsByActivityIdAndUserIdAndStatusNot(id, userId, CANCELLED)`.

> **Lo que absorbió esta tarea.** En la v2 había una `C-05` que convertía `Partner` en entidad *después*, reabriendo el modelo. No hizo falta: nació ya como entidad aquí, con `User.partner_id`, `User.status`, `User.organization` nullable, `Activity.partner`, `Activity.createdBy` y `Activity.reviewNote`. **`C-05` está absorbida en `C-02` y no queda trabajo pendiente de ella.**

> **Hecho cuando:** Con la base de datos vacía, `mvn spring-boot:run` crea las **ocho** tablas con sus claves ajenas sin un solo error, y el diagrama guardado en el repositorio coincide con lo que hay.

### `C-03` Contrato de código y contrato de API ✅ · 1 d por persona

La sesión más importante de las cuatro semanas. Salió en el PR [#179](https://github.com/Proyecto-Fundacion-Verisure/backend/pull/179), con:

**1.** Los **ocho** enumerados en `entity/enums/`:
`Role` {`ADMIN`, `EMPLOYEE`, **`PARTNER`**} · `UserStatus` {PENDING_VERIFICATION, PENDING_APPROVAL, ACTIVE, REJECTED} · `PartnerStatus` {PENDING, ACTIVE, REJECTED} · `ActivityStatus` {DRAFT, PENDING_APPROVAL, PUBLISHED, FULL, IN_PROGRESS, FINISHED, CANCELLED} · `RegistrationStatus` {WAITLISTED, CONFIRMED, REJECTED, CANCELLED, PENDING_CLOSURE, CLOSED} · `ActivityClosureStatus` {DRAFT, CLOSED} · `ProposalStatus` {NEW, ACCEPTED, REJECTED} · `Organization` {VERISURE_ES, VERISURE_GROUP}.

> **El rol se llama `PARTNER`, no `ORG`.** Es el cambio de nombre que más veces aparecía mal en la v2. Las **rutas** siguen siendo `/api/org/**` —eso no cambia—, pero el rol, el `@PreAuthorize` y el `@WithMockUser` dicen `PARTNER`.

**2.** Las **siete** firmas que cruzan dominios, creadas aunque devuelvan vacío (sección 4).
**3.** La interfaz `NotificationService` con sus **trece** métodos `notifyXxx`, más un `NotificationServiceImpl` que de momento delega en `MailDispatcher`, que solo deja traza.
**4.** `ApiError`, el `GlobalExceptionHandler` y los dieciocho códigos de dominio.
**5.** El contrato de API completo: **`docs/api-contract.md`**.
**6.** El reparto de los `@Order` de los seeders (sección 4).

> **Hecho cuando:** Las tres compilan el proyecto con el contrato dentro y el equipo de frontend tiene la lista de endpoints escrita, no de palabra.

### `C-04` Seguridad: JWT, Spring Security y CORS ✅ · 0,75 d por persona

Salió en el PR [#180](https://github.com/Proyecto-Fundacion-Verisure/backend/pull/180). **Los nombres de las clases no son los que decía la v2**, así que aquí van los reales, que es lo que hace falta para encontrarlas:

| Qué hace | Clase, en `security/` |
|---|---|
| La cadena, las rutas públicas y el reparto por rol | **`SpringConfig`** (no `SecurityConfig`) |
| Firma y valida el token | `JwtService` |
| El login · `POST /api/auth/login` | **`JWTAuthentication`**, un filtro, no un controlador |
| Lee el token de cada petición y puebla el contexto | **`JWTAuthorization`** |
| Carga el usuario por correo y comprueba su estado | `CustomAuthenticationManager` · `UserDetail` |
| Devuelve `ApiError` en 401 y en 403 | `RestAuthenticationEntryPoint` · `RestAccessDeniedHandler` · `ApiErrorWriter` |
| El cifrado de contraseñas | `PasswordEncoderConfig` |

La sesión es `STATELESS` y CSRF está desactivado: la protección CSRF existe para sesiones con cookies, y aquí la identidad viaja en una cabecera que el navegador no adjunta solo.

**Tres roles, no dos:** `ADMIN`, `EMPLOYEE` y `PARTNER`. Escribid los `@PreAuthorize` con eso en la cabeza, porque «no administradora» ya no significa «empleado».

**Las cinco rutas públicas:** `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/verify`, `POST /api/auth/resend-verification` y `POST /api/proposals`. **Todo lo demás pide token**, incluido el catálogo de actividades.

`CorsConfig` con el origen de Vite en `app.cors.allowed-origin`, los métodos, las cabeceras `Authorization` y `Content-Type`, y `Content-Disposition` expuesto para las descargas.

> **Lo que falta y no está hecho:** el **límite de peticiones por IP** en las cinco rutas públicas. No hay filtro ni dependencia, y `RATE_LIMIT_EXCEEDED` es un código que hoy nadie lanza. Está sacado a su propia tarea, `B1-22`.

> **Hecho cuando:** Una petición con token válido pasa; una con token caducado o manipulado devuelve 401 con `ApiError`; el frontend hace una llamada desde Vite y no la bloquea el navegador; y las tres saben explicar el recorrido del token en la defensa.

### `C-05` Modelo ampliado: Partner y estados de cuenta ✅ · absorbida en `C-02`

**No queda trabajo de esta tarea.** Se planificó como una sesión que reabría el modelo para convertir `Partner` en entidad, pero en `C-02` se hizo directamente bien: `Partner` con `cif` único, `User` con `partner_id` y `status`, `organization` nullable, `Activity.partner` y `Activity.reviewNote`, y los enumerados de estado de cuenta. Se mantiene el número en el documento porque otras tareas dicen «_depende de `C-05`_», y esa dependencia está satisfecha.

Lo único de su lista de «hecho cuando» que sigue vivo es una comprobación, no una tarea: **las consultas del dashboard que agrupan por organización tienen que excluir a los usuarios con rol `PARTNER`**, o aparecerá una categoría vacía en los gráficos. Se verifica en `B1-07`.

### `C-06` Seguridad con tres roles 🟡 · 0,25 d por persona

**Lo hecho:** los tres roles están en la cadena, y el reparto por prefijo también — `/api/admin/**` y `/api/dashboard/**` para `ADMIN`, `/api/org/**` para `PARTNER`, el catálogo para `EMPLOYEE` y `ADMIN`.

**Lo que falta, y es importante:** la cadena solo protege por **prefijo**. Todo lo que no lo tiene —`/api/registrations/**`, `/api/closures/**`, `/api/favorites`— cae en `anyRequest().authenticated()`, es decir, «cualquiera con token». Pero el contrato asigna roles concretos a esas rutas: `PATCH /api/registrations/{id}/accept` y `/reject` son de `ADMIN`, y `POST /api/registrations`, `GET /api/registrations/me`, los favoritos y `POST /api/closures` son de `EMPLOYEE`.

**Ese rol lo tiene que poner `@PreAuthorize` en el controlador.** `@EnableMethodSecurity` ya está activo, así que la anotación funciona; lo que no hay es nada que avise si se olvida. **Quien escriba uno de esos controladores es responsable de anotarlo**, y quien revise su PR, de comprobarlo. Hoy, sin anotación, un empleado podría aceptar su propia inscripción.

Y añadir `@WithMockUser(roles="PARTNER")` a los ejemplos del perfil de test.

> **Hecho cuando:** Un empleado que llama a `/api/org/**` recibe 403 · Una entidad que llama a `/api/admin/**` recibe 403 · **Cada endpoint sin prefijo de rol tiene su `@PreAuthorize`** · Los tests existentes siguen en verde con el rol explícito.

---

## 4 · Seis conceptos que conviene tener claros

### ¿Cómo se crean las tablas si no usamos Flyway?

Las crea **Hibernate a partir de vuestras entidades JPA**, con `spring.jpa.hibernate.ddl-auto=update` en `application.properties`.

**La consecuencia importante:** las entidades _son_ el esquema. No hay un archivo SQL que sea «la verdad»; la verdad son las clases Java. Por eso las **ocho** entidades se escribieron juntas en `C-02`: si cada una improvisa sus relaciones, Hibernate genera claves ajenas que no encajan y se descubre en la primera integración.

**Lo que hay que saber de `update`:** añade tablas y columnas nuevas, pero **nunca borra ni renombra nada**. Si cambias el nombre de un campo, la columna vieja se queda ahí para siempre. Cuando la base de datos se ponga rara, la solución es borrarla y volver a arrancar: como hay semillas, recuperar los datos cuesta diez segundos.

Para resetear el esquema a propósito hay una línea comentada en `application.properties` con `create-drop`. Cambiarla es un gesto deliberado, y hay que acordarse de dejarla como estaba antes de subir nada.

### ¿Qué es un DataSeeder?

Una clase que **inserta los datos de ejemplo al arrancar**, usando vuestros repositorios. Sin ella arrancáis y el catálogo está vacío: no podéis probar nada ni enseñar nada.

Es un `@Component` que implementa `CommandLineRunner`, anotado con **`@Profile("!prod")`** — así corre en desarrollo y en la demo, pero nunca en producción:

```java
@Component @Profile("!prod") @Order(3) @RequiredArgsConstructor
public class ActivitySeeder implements CommandLineRunner {
    private final ActivityRepository repository;

    public void run(String... args) {
        if (repository.count() > 0) return;   // ya hay datos, no dupliques
        repository.saveAll(List.of(/* ... */));
    }
}
```

**Por qué en Java y no en SQL:** usa vuestras propias entidades, así que si cambiáis un campo el compilador avisa; no hay que escribir `INSERT` a mano; y el `if (count() > 0) return` evita duplicar datos al arrancar dos veces.

**La regla del reparto:** una clase por entidad y cada una con su dueña, nunca una clase compartida. Son **siete**, y este es el orden que hay en el código:

| `@Order` | Clase | Dueña | Qué siembra |
|---:|---|---|---|
| 1 | `PartnerSeeder` | BE2 | 8 entidades colaboradoras con su CIF, por los tres `PartnerStatus` |
| 2 | `UserSeeder` | BE1 | Admins, empleados y **cuentas de entidad** por los cuatro `UserStatus` |
| 3 | `ActivitySeeder` | BE2 | 12 actividades por las cuatro líneas y por todos los estados |
| 4 | `ProposalSeeder` | BE2 | 6 propuestas: nuevas, aceptadas y rechazadas |
| 5 | `RegistrationSeeder` | BE3 | Inscripciones por todos los estados, con cola y `PENDING_CLOSURE` |
| 6 | `FavoriteSeeder` | BE3 | «Me gusta» repartidos de forma desigual |
| 7 | `ClosureSeeder` | BE1 | Cierres de participación y de actividad |

> **El orden cambió respecto de la v2, y el de ahora es el correcto.** La v2 ponía `UserSeeder` primero y `PartnerSeeder` después. No puede ser: `User` tiene `partner_id`, así que **las entidades colaboradoras van antes que los usuarios**. Después van las actividades (que apuntan a `Partner` y a `User`), las propuestas (que apuntan a una actividad si fueron aceptadas), las inscripciones y los favoritos (que apuntan a usuario y actividad), y por último los cierres, que apuntan a una inscripción.
>
> **Y no hay `OrgUserSeeder`.** La v2 lo planificaba como octavo. No hizo falta: `UserSeeder` ya siembra cuentas con rol `PARTNER` en los cuatro estados, `PartnerSeeder` cubre los tres estados de entidad, y `ActivitySeeder` tiene actividades en `DRAFT` y en `PENDING_APPROVAL`. Lo que queda de `B3-15` es una comprobación, no una clase nueva.

**El cruce que hay que tener presente:** `ClosureSeeder` es de BE1 pero necesita las inscripciones de BE3. Las lee por el repositorio del contrato, pero significa que **BE1 no puede terminar su semilla hasta que BE3 tenga la suya**. Es la única dependencia de orden real entre personas.

### ¿Qué es el problema N+1 y qué hace JOIN FETCH?

Si pides las 12 actividades del catálogo y cada una tiene que mostrar su entidad colaboradora, con la relación perezosa JPA lanza **una consulta para las 12 y luego una más por cada una**: 1 + 12 = 13 consultas para pintar una pantalla. No se nota con doce filas de semilla, sí con doscientas.

**Cómo se ve:** `spring.jpa.show-sql=true` ya está activo. Cargad el catálogo y contad los `select` en la consola. Si hay más de dos o tres, tenéis un N+1.

**La solución:** `JOIN FETCH`, o mejor una **proyección** — un `record` que JPA rellena directamente desde el `select`, como `ClosedParticipationView` o `SpotInfo`. Suele ser más rápido, porque ni siquiera construye entidades. Las tres proyecciones del proyecto viven en `repository/projection/`.

### ¿Qué es una firma que cruza dominios?

Una **firma** es la declaración de un método sin el cuerpo. «Cruza dominios» cuando **una persona la escribe y otra la usa**.

El caso concreto: el dashboard es de BE1, pero parte de los datos que necesita están en tablas de BE3. Si BE1 espera a que BE3 termine, BE1 pierde una semana. La solución: el día 1 se acuerda la firma exacta y su dueña la crea inmediatamente, aunque devuelva una lista vacía. A partir de ahí quien la consume compila contra ella y la simula con Mockito en sus tests.

**Las siete firmas, tal como están en el código.** Cinco de lectura:

| Firma | Dónde vive | La escribe | La usa |
|---|---|---|---|
| `long countByActivityIdAndStatus(Long, RegistrationStatus)` | `RegistrationRepository` | BE3 | BE1, para los confirmados de la pantalla de cierre |
| `List<Registration> findByActivityIdAndStatusOrderByQueuePosition(Long, RegistrationStatus)` | `RegistrationRepository` | BE3 | BE3 |
| `List<ClosedParticipationView> findClosedForDashboard(Integer year, String line)` | **`ParticipationClosureRepository`** | **BE1** | BE1, agregados del dashboard |
| `Optional<SpotInfo> findSpotInfo(Long activityId)` | `ActivityRepository` | BE2 | BE3, cupo, confirmadas y fecha límite sin tocar `Activity` |
| `Optional<ParticipationClosure> findByRegistrationId(Long)` | `ParticipationClosureRepository` | BE1 | BE3, para saber si una inscripción ya tiene cierre antes de permitir la baja |

> **`findClosedForDashboard` cambió de dueña, y esto sí afecta al reparto.** La v2 la ponía en `RegistrationRepository`, escrita por BE3. Vive en `ParticipationClosureRepository` y **es de BE1**, por dos razones: las horas salen de `ParticipationClosure`, no de `Registration`; y como método derivado **tumbaba el arranque de Spring**, así que necesita una `@Query` con expresión de constructor —`ClosedParticipationView` es un `record`—. Para BE3 esto es una entrega menos; para BE1, una consulta que ya no depende de nadie.

Y dos de escritura, **las únicas del proyecto**:

```java
public interface RegistrationLifecycleService {

    /** La llama BE2 al cancelar una actividad. Pasa a CANCELLED las inscripciones vivas. */
    int cancelAllForActivity(Long activityId);

    /** La llama BE1 al FINALIZAR el cierre. Pasa todas de PENDING_CLOSURE a CLOSED. */
    int closeAllForActivity(Long activityId);
}
```

Se invocan de forma **síncrona, dentro de la transacción que llama**, porque la propagación por defecto de Spring es `REQUIRED`: o se hacen las dos cosas, o no se hace ninguna.

> **`closeRegistration(registrationId)` no existe.** Se cierra por actividad, porque la Fundación cierra una vez y arrastra todas sus participaciones.

### ¿Qué es `JavaMailSender` y cómo se envían las plantillas?

`JavaMailSender` es la **interfaz de Spring para hablar con un servidor de correo**. Viene con `spring-boot-starter-mail` y se configura con propiedades, sin escribir Java.

**Mailtrap** es un buzón de pruebas: recoge todo lo que le mandéis y lo enseña en una bandeja web, pero **no lo entrega a nadie**. Es lo que permite tener treinta empleados con correos inventados sin que reboten mil mensajes.

Para enviar HTML hace falta un `MimeMessage` con `MimeMessageHelper`, y las plantillas se guardan en archivos y se rellenan con **Thymeleaf**: una plantilla base con la cabecera coral, el logotipo y el pie legal, y una por correo que solo cambie el cuerpo, con `th:replace` y fragmentos. Así son trece correos sin repetir el diseño trece veces.

> **Thymeleaf no está en el `pom.xml`.** Hay que añadir `spring-boot-starter-thymeleaf` antes de empezar `B3-09`. Está anotado dentro de esa tarea.

### ¿Por qué un `NotificationService` y no eventos de Spring?

Porque el proyecto es MVC en tres capas y un bus de eventos es una cuarta vía de comunicación que no aparece en el diagrama. Con un servicio con interfaz e implementación, **quién avisa a quién se lee en el constructor**, que es exactamente lo que se defiende en la presentación.

> **Los eventos de Spring ya no se usan en ninguna parte.** Si en una tarea de la v2 leíste «publica `ActivityCancelled`» o «publica un evento», eso está corregido en esta versión: se llama al método `notifyXxx` que corresponda. No hay `@TransactionalEventListener` ni `ApplicationEventPublisher` en el proyecto.

**Son trece métodos**, todos `void`, todos con el identificador como único argumento:

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

> **No hay un método de acuse de recibo de propuesta.** La v2 daba por hecho un correo al enviar el formulario público. Si se quiere, hay que añadir un decimocuarto método al contrato por PR; si no, `B2-06` no manda ningún correo. Está en la lista de enmiendas pendientes al final de este documento.

### La trampa que trae este cambio: la transacción

**Es el único punto donde el servicio es peor que los eventos, y hay que resolverlo a mano.**

Con `@TransactionalEventListener(AFTER_COMMIT)`, Spring garantizaba que el correo solo salía **si la transacción confirmaba**. Un servicio llamado directamente no garantiza nada: si lo llamáis dentro de un método `@Transactional` y algo falla tres líneas después, la base de datos se deshace **pero el correo ya salió**.

**La solución acordada: el controlador orquesta.** El servicio hace su trabajo dentro de su transacción y devuelve; el controlador, si no ha saltado ninguna excepción, llama al aviso. Así está escrito ya en `ActivityClosureController`:

```java
// service/ActivityClosureServiceImpl.java
@Transactional
public ActivityClosureResponse finalizeClosure(Long id) { ... }   // el servicio NO avisa

// controller/ActivityClosureController.java
var body = activityClosureService.finalizeClosure(id);   // ya confirmó
notificationService.notifyActivityClosed(id);
return ResponseEntity.ok(body);
```

En las tareas programadas orquesta el método `@Scheduled`, que **por eso no lleva `@Transactional`**.

Como red de seguridad, `NotificationServiceImpl` difiere el envío a `afterCommit()` si detecta una transacción abierta, y el envío real lo hace **`MailDispatcher`, un bean aparte con `@Async`**. Es una protección, no un permiso para saltarse la regla.

**Dos trampas de _proxy_, que no dan ningún error.** `@Transactional` y `@Async` viven en un _proxy_ alrededor del bean: una llamada de un método a otro **de la misma clase** no pasa por él y la anotación **se ignora en silencio**. Por eso `MailDispatcher` es un bean separado, y por eso no vale partir un servicio en un método público sin `@Transactional` que llame a uno interno que sí la lleve. No da error, no avisa, y en la demo funciona.

**Un caso que la regla no cubre sola:** cuando el destinatario nace dentro de la transacción y su identificador no está en la ruta. Es lo que pasa con `notifySpotReleased`, que va a quien ascendió de la cola. El servicio tiene que devolver ese dato:

```java
public record CancelResult(RegistrationResponse body, Long promotedRegistrationId) {}
```

---

## 5 · Glosario: una palabra por cosa

La misma tabla en el documento de backend y en el de frontend. Cuando una palabra aparece aquí, **esa es la palabra**: en el código, en la interfaz, en los mensajes de error, en el tablero y en los commits.

No es purismo. Hay **tres pares de palabras que se parecen y significan cosas distintas**, y confundirlas no da un error de compilación: da un dashboard que agrupa por lo que no es.

| En castellano | En código | Qué es |
|---|---|---|
| **Inscripción** | `Registration` | La solicitud de una persona para participar, en cualquiera de sus seis estados. **Nunca «registro»**: en castellano «registro» es una traza o un histórico. |
| **Traza · histórico** | `log` | Lo que se escribe con `@Slf4j`. Es el otro sentido de «registro», y por eso no usamos esa palabra para ninguno de los dos. |
| **Plaza** | `spot` | Un hueco en el aforo de una actividad. Nunca `seat`. |
| **Aceptada** | `accepted` | Booleano: la administradora ha revisado la solicitud y le parece bien. **No es un estado**: una inscripción puede estar aceptada y seguir en la cola esperando plaza. |
| **Organización** | `Organization` | A qué parte de Verisure pertenece una persona: `VERISURE_ES` o `VERISURE_GROUP`. Es lo que antes llamábamos «sede». |
| **Entidad · entidad colaboradora** | `Partner` | La ONG con la que se colabora. Es una **entidad con tabla propia** y `cif` único, no un texto. **Nunca «organización» a secas**, que es la colisión más cara del proyecto. |
| **Cuenta de entidad** | `User` con **`role = PARTNER`** | Una persona que representa a una entidad. Pertenece a un `Partner` y pasa por cuatro estados. Una entidad puede tener más de una cuenta, todas con el mismo CIF. **El rol se llama `PARTNER`; las rutas, `/api/org/**`.** |
| **En revisión** | `PENDING_APPROVAL` | Actividad terminada y enviada, esperando a la Fundación. **No es lo mismo que «borrador»**: `DRAFT` significa «se está escribiendo». Van a bandejas distintas. |
| **Devolver** | `return` | La administradora rechaza **una actividad propuesta por una entidad** pidiendo cambios, con comentario obligatorio. **Los cierres no se devuelven.** |
| **Propuesta** | `Proposal` | Lo que llega por el formulario público. Tres estados: `NEW`, `ACCEPTED`, `REJECTED`. |
| **Cierre de participación** | `ParticipationClosure` | Lo que rellena **el empleado**: horas, valoración y evidencia. Uno por inscripción. **Las horas que declara son las buenas**: nadie las corrige. |
| **Cierre de actividad** | `ActivityClosure` | Lo que cierra **la Fundación**. **Uno por actividad, no uno por persona.** Dos estados, que son los dos botones de la pantalla: `DRAFT` y `CLOSED`. |
| **Informe** | `report` | El dashboard y los descargables. **Nunca es una tabla.** Para el formulario del empleado se dice **cierre**. |
| **Rechazada** | `REJECTED` | Estado final de una inscripción o de una propuesta. **Sin motivo asociado.** |
| **Cancelar** | `cancel` | Una sola palabra y un solo endpoint para las dos vías. No se «borra» nada: la fila se queda en `CANCELLED`. |

> **La entidad `Partner` y el rol `PARTNER` son cosas distintas que comparten nombre.** `Partner` es la entidad colaboradora —una fila con su CIF—; `PARTNER` es el rol de las personas que trabajan en ella.
>
> **`ActivityStatus.DRAFT` y `ActivityClosureStatus.DRAFT` tampoco son lo mismo.** El primero es una actividad que se está escribiendo; el segundo, un cierre a medio rellenar.

---

## 6 · Los errores

Cuando una tarea dice «devuelve 400 con código de dominio» quiere decir que, además del código HTTP, el cuerpo lleva un **código propio en mayúsculas** que identifica qué regla se ha incumplido. `400 Bad Request` a secas no le sirve al frontend: no puede distinguir «te faltan campos» de «esta actividad todavía no ha terminado».

**La forma, única para todos los casos**, incluidos los de validación por campo. Está en `exception/ApiError.java`:

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
  "timestamp": "2026-09-07T10:14:03Z",
  "path": "/api/auth/register",
  "fields": {
    "cif":  ["debe tener 9 caracteres", "solo admite letras y números"],
    "name": ["no puede estar vacío"]
  }
}
```

Tres cosas que la v2 tenía mal y conviene fijar:

1. **No lleva `status`.** El código HTTP ya viaja en la respuesta; repetirlo en el cuerpo es una copia que se puede desincronizar.
2. **`fields` es un `Map<String, List<String>>`, no un mapa de cadenas.** Un mismo campo puede incumplir dos validaciones a la vez —un CIF que falla `@Size` y `@Pattern`— y con un solo mensaje por campo uno de los dos se pierde de forma no determinista.
3. **Lleva `path`**, que es lo que permite localizar la llamada que falló sin cruzar registros.

`fields` es `null` salvo en `VALIDATION_ERROR`.

### Los dieciocho códigos de dominio

Están en `exception/ErrorCode.java`, cada uno con su `HttpStatus`, para que `GlobalExceptionHandler` necesite un solo manejador para todos. **Nadie escribe el código como cadena suelta en su servicio.**

| Código | HTTP | Cuándo | Quién lo lanza |
|---|---:|---|---|
| `VALIDATION_ERROR` | 400 | Bean Validation falló · trae `fields` | todas |
| `DEADLINE_PASSED` | 400 | Pasó la fecha límite de inscripción | BE3 |
| `ACTIVITY_NOT_FINISHED` | 400 | Intentas cerrar antes de que la actividad termine | BE1 |
| `INVALID_DATE_RANGE` | 400 | Fecha de fin anterior a la de inicio | BE2 |
| `NOT_OWNER` | 403 | Pides un recurso que no es tuyo | BE1 |
| `ACCOUNT_NOT_VERIFIED` | 403 | Falta confirmar el correo | BE1 |
| `ACCOUNT_PENDING_APPROVAL` | 403 | La Fundación aún no ha aprobado la cuenta | BE1 |
| `ACCOUNT_REJECTED` | 403 | La cuenta fue rechazada | BE1 |
| `ALREADY_REGISTERED` | 409 | Ya tienes una inscripción que no cancelaste tú | BE3 |
| `REGISTRATION_NOT_CONFIRMED` | 409 | Cierras una inscripción que no estaba confirmada | BE1 |
| `ACTIVITY_NOT_CLOSED` | 409 | Pides el certificado de una actividad sin cerrar | BE1 |
| `CLOSURE_ALREADY_CLOSED` | 409 | Corriges tu cierre con la actividad ya cerrada | BE1 |
| `ACTIVITY_FINISHED` | 409 | Editas una actividad ya finalizada | BE2 |
| `ACTIVITY_NOT_EDITABLE` | 409 | Ya está enviada a revisión o publicada | BE2 |
| `CIF_ALREADY_REGISTERED` | 409 | Ese correo ya tiene cuenta en esa entidad | BE1 |
| **`PROPOSAL_ALREADY_DECIDED`** | 409 | La propuesta ya se aceptó o rechazó | BE2 |
| `VERIFICATION_EXPIRED` | 410 | El enlace del correo caducó | BE1 |
| **`RATE_LIMIT_EXCEEDED`** | 429 | Límite por IP en las rutas públicas · `B1-22` | BE1 |

Los genéricos de HTTP —`UNAUTHORIZED` 401, `FORBIDDEN` 403, `NOT_FOUND` 404, `MALFORMED_REQUEST` 400, `PAYLOAD_TOO_LARGE` 413, `UNSUPPORTED_MEDIA_TYPE` 415, `METHOD_NOT_ALLOWED` 405, `INTERNAL_ERROR` 500— **no son códigos de dominio** y no están en el enumerado, pero los traduce el mismo manejador y devuelven el mismo `ApiError`.

> 🚫 **`ACTIVITY_FULL` no existe y no debe aparecer nunca.** Aceptar una inscripción no falla por aforo: con hueco confirma, sin hueco conserva la cola con `accepted = true`.
>
> 🚫 **`REPORT_ALREADY_SUBMITTED` tampoco.** El cierre de participación no tiene estados, así que no hay reenvío que rechazar.
>
> 🚫 Tampoco pueden aparecer `Enrollment`, `SeatService` ni `/api/enrollments`. Los nombres son `Registration` y `SpotService`.

---

## 7 · El contrato de API

**Está en `docs/api-contract.md`, y ese es el único sitio.**

Son 52 endpoints en once bloques, con su método, su ruta, su rol, lo que reciben, lo que devuelven y sus errores. Se acordó en `C-03` con el equipo de frontend delante, porque es lo que les permite escribir su capa `api/` contra datos simulados sin esperar al backend.

En la v2 este documento tenía además su propia tabla de rutas. **Se ha eliminado**, porque se había quedado a medio camino: decía `/api/activities/published` para un catálogo que ahora es `/api/activities` con token, colgaba la administración de actividades de `/api/activities/**` en vez de `/api/admin/activities/**`, y ponía los favoritos en `/api/activities/{id}/favorite` en vez de `/api/favorites`. Dos listas de endpoints que se separan silenciosamente es justo lo que C-03 vino a arreglar.

**Si te falta un endpoint o un campo, no te lo inventes:** se pide el cambio, se actualiza `docs/api-contract.md` por PR, y luego se implementa.

---

## 8 · Reglas de trabajo en paralelo

**1. Nadie toca el paquete de otra.** Si necesitas un cambio en algo que no es tuyo, se pide por el canal y lo hace su dueña. Cambiar el código de otra persona a mitad de semana es la vía más rápida a un conflicto de fusión que se come una tarde.

**2. Cada entidad tiene una sola dueña, y las relaciones son unidireccionales.** `Activity` es de BE2 y solo BE2 la edita; `Registration` es de BE3 y solo BE3 la edita. Para eso, las relaciones se declaran **solo en el lado que tiene la clave ajena**. Cuando necesitéis las inscripciones de una actividad, se piden al repositorio.

**3. Si cambias una entidad, dilo en el punto de la mañana.** Sin migraciones no hay archivo que avise: la base de datos de las demás se actualiza sola al arrancar y, si has quitado o renombrado algo, `ddl-auto=update` no lo borra y queda una columna fantasma.

**4. Una clase de seeder por entidad, nunca una compartida.** Siete clases, cada una con su `@Order` (sección 4). Una sola clase compartida garantiza conflicto en cada PR.

**5. Entre dominios solo se habla por lo que esté en el contrato.** Hay **tres** cosas que puedes necesitar de otro dominio, y cada una tiene su vía:
**Leer** → el repositorio del contrato. **Avisar** → `NotificationService`. **Escribir** → una operación publicada en el contrato; en este proyecto solo hay dos, las de `RegistrationLifecycleService`.
Lo que no se hace **nunca** es cambiar tú el estado de una entidad ajena: ni `registration.setStatus(...)` desde BE1 o BE2, ni una consulta de actualización masiva sobre una tabla de otra. La regla real no es «no llames al código de nadie», es **no escribas tú una regla de transición que pertenece a otra**, porque acaba existiendo en dos sitios que se separan con el tiempo.

**6. Los avisos son para efectos, no para mantener la coherencia.** `NotificationService` sirve para mandar un correo o dejar una traza: cosas que pueden fallar sin que los datos queden mal. **No sirve para un cambio de estado.**
De ahí salen las dos reglas, que son opuestas a propósito: **las escrituras cruzadas van dentro de la transacción** de quien llama, síncronas, porque los datos tienen que quedar bien o no quedar; **los avisos van fuera**, después de que la transacción confirme, porque un correo que sale de más no se puede recoger.

**7. Cada test crea sus propios datos.** Nada de tests que dependen de la semilla de otra persona.

**8. Si falta algo del contrato, se amplía, no se improvisa.** PR pequeño, revisado por las tres, mismo día. Lo que no vale es que cada una cree su propia versión del mismo enumerado o su propia ruta para lo mismo.

**9. El rol lo pone `@PreAuthorize` cuando la ruta no tiene prefijo.** La cadena solo protege `/api/admin/**`, `/api/dashboard/**`, `/api/org/**` y el catálogo. Todo lo demás llega como «cualquiera con token», así que **el `@PreAuthorize` de tu controlador es la única barrera que hay**. Si se olvida, no falla nada: simplemente cualquiera puede llamarlo.

---

## BE1 · Acceso, cierres e informes

Quién eres y lo que se mide. Cubre H1 · H2 · H16 · H17 · H18 · H19 · H20 · H21 · H22 · H25 · H26 · H30.

**Es dueña de:** `User` · `Partner` · `ParticipationClosure` · `ActivityClosure` · `AuthService` · `OrgAccountService` · `ParticipationClosureService` · `ActivityClosureService` · `CertificateService` · `DashboardService` · `CsvExportService` · `PdfExportService`

_Además de las tareas conjuntas._

### Semana 1

#### `B1-01` Entidad User, repositorio y semilla ✅ · 0,75 d

Hecha. `User` con Lombok —`@Getter`, `@Setter` y constructores, **nunca `@Data`**—, `UserRepository.findByEmail`, y `UserSeeder` con **`@Order(2)`**, después de `PartnerSeeder`. Siembra administradoras, empleados repartidos por departamento y organización, y **cuentas de entidad en los cuatro `UserStatus`**, con las contraseñas ya cifradas.

#### `B1-02` AuthController: login, logout y perfil 🟡 · 0,5 d

**Lo hecho:** el login. Y conviene saber dónde está, porque no es donde se espera: lo sirve el filtro **`JWTAuthentication`**, no un controlador. Ahí se deserializa el JSON `{email, password}`, se emite el token y se devuelve `AuthResponse`. Los errores de estado de cuenta y el 401 genérico salen de `unsuccessfulAuthentication`.

**Lo que falta:** `AuthController` no existe y `UserController` está vacío. Quedan `POST /api/auth/logout` y `GET /api/auth/me`.

`logout` responde 204 y deja constancia en la traza. **No revoca el token, y no puede:** un JWT es sin estado, así que no hay ninguna fila que borrar. El cierre de sesión real ocurre en el cliente. Por eso la caducidad es corta, **2 horas**: **la caducidad es la política de revocación**. Se descartó la lista de tokens revocados a propósito, porque obliga a una lectura de base de datos en cada petición, que es justo lo que se evita eligiendo JWT. El precio, y hay que saber decirlo en la defensa, es que un token copiado antes de salir sigue sirviendo hasta que caduca.

`GET /api/auth/me` devuelve `UserResponse`, que ya existe.

> **Hecho cuando:** Las tres llamadas funcionan desde Postman con las cuentas de la semilla.

#### `B1-14` Registro de entidad con deduplicación por CIF ⬜ · 1,0 d

`POST /api/auth/register`, público. `RegisterOrgRequest` con Bean Validation: nombre, CIF, contacto, correo, teléfono, contraseña y consentimiento con `@AssertTrue`.

**La deduplicación es la parte que importa.** Si el CIF no existe, se crean `Partner` en `PENDING` y `User` en `PENDING_VERIFICATION`. Si el CIF **ya existe**, no se crea una entidad nueva: el usuario se asocia a la que hay y queda igualmente pendiente de aprobación. Sin esa regla, dos personas de Cáritas producen dos entidades casi homónimas, sus horas se parten en dos y el ranking del dashboard miente sin que nadie lo note.

`CIF_ALREADY_REGISTERED` cuando el correo ya tiene cuenta en esa entidad.

> **Hecho cuando:** Registrarse con un CIF nuevo crea entidad y usuario, los dos pendientes · Con un CIF existente **no** crea una segunda entidad · Sin marcar el consentimiento, 400 señalando la casilla.

### Semana 2

#### `B1-03` ParticipationClosure: el cierre que rellena el empleado 🟡 · 1,5 d

**Lo hecho:** la entidad y el repositorio, con `registration_id` **único** — nunca hay dos cierres para la misma participación, y por eso `findByRegistrationId` devuelve uno y no una lista. Las interfaces `ParticipationClosureService.submit(...)` y `.getById(...)` existen, vacías.

**Lo que falta:** el controlador, el servicio y **los campos de los DTO**, que siguen siendo `record` vacíos con `TODO C-03`. Esto es lo que bloquea a frontend: sin campos no pueden mockear ninguna pantalla de cierre. **Es lo primero que hay que hacer de esta tarea, antes que el servicio.**

**Los campos de la entidad, y los que deliberadamente no están:**

```
id · registration_id (ÚNICO) · actual_hours · rating (1..5)
comment · evidence_url · submitted_at
```

**No hay `validated_hours` y no hay estados.** Las horas que declara el empleado **son las buenas**: nadie las corrige. Si a la administradora no le cuadran, lo escribe en las notas del cierre de la actividad. Y no hay máquina de estados porque la fila existe o no existe.

**`POST /api/closures`**, multipart: la parte `request` con el JSON y una parte opcional `evidence`.

```
CreateClosureRequest { registrationId, actualHours, rating (1..5), comment?, evidenceConsent }
```

El `registrationId` va **en el cuerpo, no en la ruta**: el cierre todavía no existe, así que no tiene identificador propio. La evidencia acepta **PDF, JPG y PNG, máximo 10 MB**; si hay archivo, `evidenceConsent` debe ser `true`.

Antes de aceptar comprueba dos cosas leyendo por el repositorio del contrato: que la actividad ya **terminó** y que la inscripción estaba **confirmada**. Si no, 400 `ACTIVITY_NOT_FINISHED` o 409 `REGISTRATION_NOT_CONFIRMED`.

> ⚠️ **Corregir un cierre está sin acordar.** Esta tarea decía que un segundo `POST` sobre la misma inscripción **actualiza** y responde 200 en vez de 201. El contrato solo documenta 201 y `CLOSURE_ALREADY_CLOSED`. Son dos comportamientos distintos para el mismo endpoint: hay que decidir cuál vale y enmendar el que sobre. Está en la lista de enmiendas pendientes.

> **Hecho cuando:** Los DTO tienen campos y frontend puede mockear · Un cierre válido se guarda · Enviarlo antes de que la actividad termine devuelve el código correcto y no crea nada · Con la actividad ya cerrada, 409.

#### `B1-04` ActivityClosure: la Fundación cierra la colaboración 🟡 · 1,75 d

**Lo hecho:** la entidad, el repositorio, la proyección `ActivityClosureAggregates`, la interfaz `ActivityClosureService` y **`ActivityClosureController` entero**, con sus cuatro rutas y con el aviso ya orquestado en el sitio correcto. **Lo que falta es el `Impl`**, que hoy devuelve `null`, y los campos de los DTO.

**La administradora no valida doce formularios uno por uno.** Cierra la actividad **una vez**, mirando los totales.

Entidad `ActivityClosure`, **una por actividad**, con `activity_id` único:

```
id · activity_id (ÚNICO) · collaboration_rating (1..5)
closing_notes · lessons_learned · status · closed_at
```

> **`collaboration_rating` va de 1 a 5**, no de 1 a 4 como decía la v2. El contrato dice 5 y es la misma escala que la valoración del empleado; tener dos escalas distintas en la misma pantalla es una fuente de errores gratuita.

**Dos estados, que son los dos botones de la pantalla:** `DRAFT` («Guardar borrador») y `CLOSED` («Finalizar colaboración»).

Las cuatro rutas, todas bajo `/api/admin/` para que la única regla de seguridad que necesiten sea la de `ADMIN`:

| | |
|---|---|
| `GET /api/admin/activities/pending-closure` | Las actividades finalizadas sin cierre en `CLOSED`, por antigüedad |
| `GET /api/admin/activities/{id}/closure` | El borrador **más los agregados que la admin necesita para decidir** |
| `PUT /api/admin/activities/{id}/closure` | Guarda el borrador, tantas veces como haga falta |
| `PATCH /api/admin/activities/{id}/closure/finalize` | Cierra. **No se deshace** |

Los agregados son horas previstas frente a reportadas, voluntarios confirmados frente a los que cerraron, y cuántas evidencias hay. `expectedHours` es `activity.hours × confirmedVolunteers`; el resto sale de `findAggregatesByActivityId`, que ya está escrita.

**`finalize`, dentro de `@Transactional`:**

```
el cierre pasa a CLOSED y se sella closed_at
registrationLifecycleService.closeAllForActivity(activityId)
  → todas sus inscripciones pasan a CLOSED
── aquí termina la transacción ──
notificationService.notifyActivityClosed(activityId)
```

**Fíjate dónde está la línea.** El aviso va **fuera** del método `@Transactional` — en el controlador, que es donde ya está escrito. Si lo metes dentro y algo falla después, habrás avisado a doce personas de un cierre que se deshizo.

A partir de aquí las horas cuentan en los dos dashboards y sus participantes pueden pedir el certificado. **No hay «devolver»:** la gente participó de verdad, no hay nada que rechazar.

> **Hecho cuando:** Guardar borrador deja el cierre editable; finalizar pasa la actividad y todas sus inscripciones a cerradas de una vez; los agregados cuadran con la suma de los cierres individuales; y finalizar dos veces devuelve 409.

#### `B1-05` Revisión de PR y tablero ⬜ · 0,5 d

Revisar los PR de BE2 y BE3 mirando que el controlador no decida, que no salga una entidad por HTTP, que las excepciones estén tipadas **y que cada endpoint sin prefijo de rol lleve su `@PreAuthorize`**.

#### `B1-15` Verificación de correo ⬜ · 1,0 d

`GET /api/auth/verify?token=`, público. Token de un solo uso con caducidad de 24 horas, guardado con su fecha y su marca de usado. **Nunca el identificador del usuario en claro**: un token adivinable permite verificar cuentas ajenas.

> **Esto necesita almacenamiento que hoy no existe.** Ninguna entidad guarda el token de verificación ni su caducidad. Hay que añadir los campos a `User` o crear una tabla propia, y decidirlo antes de empezar. Como no hay migraciones, es tocar la entidad y rearrancar.

Al verificar, el usuario pasa de `PENDING_VERIFICATION` a `PENDING_APPROVAL`. Verificar **no** da acceso: solo confirma que el correo existe. `POST /api/auth/resend-verification` llama a `notifyVerificationRequested`.

Códigos: `VERIFICATION_EXPIRED` (410) y `ACCOUNT_NOT_VERIFIED` (403).
_Depende de `B1-14`._

> **Hecho cuando:** El enlace verifica y deja la cuenta pendiente de aprobación · El mismo enlace usado dos veces falla · Un token caducado devuelve 410 · Verificado pero sin aprobar, el login sigue rechazando.

#### `B1-16` Aprobación y rechazo de cuentas de entidad ⬜ · 0,75 d

`GET /api/admin/org-accounts` paginado y filtrado por estado. Devuelve entidad, CIF, contacto, fecha de solicitud y si verificó el correo.

`PATCH .../approve` deja usuario y entidad en `ACTIVE`; `PATCH .../reject`, en `REJECTED`. Los dos dentro de `@Transactional`, y el aviso —`notifyOrgAccountApproved` / `notifyOrgAccountRejected`— **fuera**, desde el controlador.

**Rechazo con CIF existente:** si el `Partner` ya estaba `ACTIVE` porque tiene otra cuenta aprobada, rechazar la nueva solicitud cambia **únicamente ese `User`**. Solo pasa también el `Partner` a `REJECTED` cuando era nuevo, estaba en `PENDING` y no tiene ninguna cuenta activa.
_Depende de `B1-15`._

> **Hecho cuando:** Aprobar deja entrar; rechazar no · Los dos llaman a su método de `NotificationService` fuera de la transacción · Aprobar el segundo usuario de una entidad existente no crea una segunda entidad.

#### `B1-17` Login que distingue el estado de la cuenta 🟡 · 0,5 d

**Lo hecho:** `JWTAuthentication.unsuccessfulAuthentication` ya devuelve los tres códigos si el `CustomAuthenticationManager` lanza `AccountStatusException`. **Lo que falta** es comprobar que los tres estados se disparan de verdad, una vez existan cuentas en cada uno.

El login devuelve un código distinto por estado: `ACCOUNT_NOT_VERIFIED` · `ACCOUNT_PENDING_APPROVAL` · `ACCOUNT_REJECTED`, los tres con 403. Credenciales incorrectas siguen dando **401 genérico**, con el mismo mensaje exista el correo o no: decir «ese correo no está registrado» permitiría averiguar quién tiene cuenta probando direcciones.

**La distinción importa:** los tres códigos solo aparecen **después de acertar la contraseña**, así que no filtran qué correos existen.

### Semana 3

#### `B1-06` CertificateService ⬜ · 0,75 d


El certificado es el único documento del MVP que sale de la plataforma con un nombre propio dentro. Por eso tiene dos comprobaciones y no una.

`GET /api/closures/{id}/certificate`:

1. **Propiedad**: el usuario del token tiene que ser el de la inscripción del cierre. Si no, 403 `NOT_OWNER`. Sin esto, cambiando el número de la dirección cualquiera se descarga el certificado de otra persona: es una brecha de datos personales, no un fallo estético.
2. **Actividad cerrada**: su `ActivityClosure` tiene que estar en `CLOSED`. Si no, 409 `ACTIVITY_NOT_CLOSED`.
3. Devuelve `CertificateResponse` con nombre completo, título, entidad, línea, fechas, **las horas que declaró la persona**, fecha de expedición y una **referencia** del tipo `CERT-2026-0418`.

**Qué NO hace:** no genera el PDF. El diseño vive en el frontend y se descarga con el diálogo de impresión. Generar PDF en servidor habría costado día y medio que no tenemos, y el resultado sería peor.

_La referencia sale de `B1-21`._

> **Hecho cuando:** Con tu propio cierre, 200 con todos los campos y **la misma referencia siempre** · Con el de otra persona, 403 · Con una actividad sin cerrar, 409.

#### `B1-21` La referencia del certificado ⬜ · 0,25 d · **tarea nueva**

`B1-06` da por hecho que la referencia se genera una sola vez, al cerrar la actividad, y se guarda. **No hay ninguna columna donde guardarla**: ni `ActivityClosure` ni `ParticipationClosure` la tienen.

Hay que añadir el campo a `ParticipationClosure` —es una referencia por participación, no por actividad— y rellenarlo dentro de `closeAllForActivity`, o en el `finalize` de `B1-04`, en la misma transacción.

**Por qué no se genera al pedir el certificado:** cada descarga daría un número distinto y dejaría de servir para verificar nada.
_Depende de `B1-04`._

> **Hecho cuando:** La columna existe · Finalizar un cierre la rellena para todas sus participaciones · Pedir el certificado dos veces devuelve la misma referencia.

#### `B1-07` DashboardService: agregados sobre actividades cerradas ⬜ · 1,75 d

Horas donadas, voluntarios activos, actividades finalizadas, personas beneficiadas y «me gusta» acumulados. Agregados por departamento, por **organización** y por línea de acción. Tabla de entidades colaboradoras.

**Todo restringido a inscripciones `CLOSED`**, leyendo por `findClosedForDashboard`, que **ya está escrita y verificada** en `ParticipationClosureRepository`: devuelve `ClosedParticipationView(department, organization, line, actualHours, endDate)` filtrando por año y línea. Es tuya, no dependes de nadie.

**Los agregados por organización excluyen a los usuarios con rol `PARTNER`**, o aparecería una categoría vacía en los gráficos. Es lo único que quedó vivo de `C-05`.

Índices en las columnas por las que se agrupa.

> **Hecho cuando:** Los números cuadran a mano con los datos de la semilla, y una inscripción que no esté en `CLOSED` no aparece en ninguno.

#### `B1-08` Filtros y las dos exportaciones en CSV ⬜ · 1,5 d

`year` y `line` en todas las consultas. `GET /api/dashboard/participations.csv` con una fila por participación cerrada — identificador **seudonimizado**, sin nombre ni correo —, y `GET /api/dashboard/partners.csv` con las entidades, sus actividades y sus horas.

**Ojo con la ruta:** son `/api/dashboard/participations.csv`, sin `/export/` por medio.

BOM UTF-8 y punto y coma para que Excel los abra con los acentos bien. `Content-Disposition` ya está expuesto en CORS.

> **Hecho cuando:** Los dos archivos se abren en Excel sin caracteres raros y un empleado que pruebe la dirección recibe 403.

#### `B1-09` Revisión de PR ⬜ · 0,25 d

Esta semana entra la lógica de cupo de BE3. Revísala con lupa: es la parte con más riesgo del proyecto.

#### `B1-18` Dashboard de la entidad ⬜ · 1,25 d

`GET /api/org/dashboard`. Reutiliza los agregados, **filtrados por el `partner` del token**: horas recibidas, actividades finalizadas, voluntarios distintos y evolución por año y por línea.

Cuenta **solo actividades cerradas**, igual que el de la Fundación: si contara las que aún no ha cerrado, los dos paneles darían números distintos para lo mismo y no habría forma de explicarlo.

Ojo al **N+1**: arrancad con `show-sql` y contad.

> **Hecho cuando:** Los números cuadran con los del dashboard de la Fundación filtrado por esa entidad · Una entidad sin actividades cerradas ve ceros, no un error · La consulta no dispara una petición por actividad.

### Semana 4

#### `B1-10` PDF con la representación gráfica del dashboard ⬜ · 0,5 d

`GET /api/dashboard/report.pdf` con los KPIs y los gráficos, respetando los filtros. **Es la primera pieza que se cae si vais mal de tiempo.**

#### `B1-22` Límite de peticiones en las rutas públicas ⬜ · 0,5 d · **tarea nueva**

`C-04` y `C-06` lo daban por hecho y no se hizo. Las **cinco rutas públicas** están abiertas a internet sin ningún límite, y `RATE_LIMIT_EXCEEDED` es hoy un código que nadie lanza.

Un filtro en `security/` con un contador por IP en memoria — no hace falta Redis para un MVP — aplicado a `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/verify`, `POST /api/auth/resend-verification` y `POST /api/proposals`. Devuelve **429** con `ApiError` y código `RATE_LIMIT_EXCEEDED`.

El de `resend-verification` limita además **por correo**, no solo por IP: si no, se puede usar para inundar el buzón de otra persona.

**Es de BE1 porque toca `security/`, que es donde vive el acceso.** Avísalo en el punto de la mañana antes de tocarlo, porque `SpringConfig` lo escribieron las tres.

> **Hecho cuando:** El intento número once desde la misma IP en una hora recibe 429 con `ApiError` · Las cinco rutas están cubiertas · Una IP distinta no se ve afectada.

#### `B1-11` Bloque de pruebas · ver la sección 9 ⬜ · 2,5 d

#### `B1-12` README de arranque y seguridad, y colección de Postman ⬜ · 1,0 d

Cómo levantar el proyecto desde cero: requisitos, base de datos, variables de `.env`, cómo se ejecutan los tests, y el recorrido del token explicado. Colección de Postman de acceso, cierres y dashboard, con el token guardado automáticamente tras el login.

> **Hecho cuando:** Alguien que no ha tocado el proyecto lo arranca siguiendo el README, sin preguntar.

#### `B1-13` Corrección de errores y ensayo de tu parte de la demo ⬜ · 0,5 d

Guion de dos minutos: acceso con los dos roles, un cierre y cómo ese dato aparece en el dashboard y en la exportación.

#### `B1-19` Barrera de datos personales en `/api/org/**` ⬜ · 0,5 d

**Es una decisión tomada y no se recorta.** Una entidad no ve quién se ha apuntado: ni nombres, ni correos, ni departamentos, ni horas individuales. Solo recuentos. No son datos suyos: son de empleados de Verisure, y la entidad es un tercero.

**Se implementa como barrera explícita, no como un `if` dentro de un mapper.** DTO propios en un paquete `org` que sencillamente **no tienen** esos campos, y mappers propios. Lo que no existe en la clase no se puede filtrar por descuido en un refactor.

Y un test de integración que recorra **todos** los endpoints de `/api/org/**` y verifique que ninguna respuesta contiene un correo ni un nombre de persona.

Todas esas rutas resuelven el `partnerId` **desde la sesión, nunca desde un parámetro**.
_Depende de `B1-18`._

> **Hecho cuando:** Ningún DTO del paquete `org` tiene campos de identidad personal · El test recorre todos los endpoints del rol y pasa · Añadir un endpoint nuevo sin DTO propio hace fallar el test.

#### `B1-20` Pruebas del rol entidad · BE1 ⬜ · 0,5 d

Unitarias del registro (CIF nuevo, CIF existente, consentimiento ausente), del token de verificación (válido, usado, caducado) y del login por cada estado de cuenta. Integración: **el test de la barrera de datos personales**, que es el que no puede faltar.

---

## BE2 · Catálogo: actividades y propuestas

Lo que la Fundación publica y lo que las entidades proponen. Cubre H4 · H5 · H6 · H7 · H10 · H11 · H27 · H28 · H29 · H31.

**Es dueña de:** `Activity` · `Proposal` · `ActivityService` · `ProposalService` · `FileStorageService`

### Semana 1

#### `B2-01` Entidad Activity, repositorio y semilla 🟡 · 1,25 d

**Lo hecho:** la entidad completa con `ActivityStatus`, `partner`, `createdBy`, `reviewNote` e índices; **`findSpotInfo` con su `@Query` y su proyección `SpotInfo`**, que es la firma que consume BE3, ya funcionando y no vacía; y `ActivitySeeder` con **`@Order(3)`** — doce actividades por las cuatro líneas, cubriendo `DRAFT`, `PENDING_APPROVAL`, `PUBLISHED`, `FULL`, `IN_PROGRESS`, `FINISHED` y `CANCELLED`.

**Lo que falta:** el resto de `ActivityRepository` — el listado por estado con el recuento de «me gusta» para la administradora, y la búsqueda por título o entidad.

> ⚠️ **`Activity` no tiene campo de requisitos.** `B2-05` habla de un formulario con «dirección y requisitos»; la entidad tiene `location` y nada más. Si el formulario los necesita, hay que añadir la columna; si no, quitarlo de la descripción de la pantalla. Está en las enmiendas pendientes.

#### `B2-12` PartnerSeeder ✅ · 0,25 d

Hecha, con **`@Order(1)`** — el primero de todos, porque `User`, `Activity` y `Proposal` apuntan a él.

**Aclaración de propiedad:** la entidad `Partner` es de **BE1** —nace en el registro y su ciclo lo lleva la aprobación de cuentas—, pero el seeder es tuyo porque es tu catálogo de referencia. Sembrar no es poseer: un seeder corre antes de que la aplicación atienda peticiones y no aplica ninguna regla de negocio, así que **no es una escritura que cruce dominios**.

Ocho entidades con CIF ficticio pero de formato válido, cubriendo los tres `PartnerStatus`.

### Semana 2

#### `B2-02` Crear actividad: validación, mapeo con MapStruct y controlador ⬜ · 2,0 d

**Validación.** `CreateActivityRequest` con Bean Validation y un validador propio `@ValidDateRange` que impide fin anterior a inicio y límite de inscripción posterior al inicio, devolviendo `INVALID_DATE_RANGE`.

**Mapeo con MapStruct.** Genera en tiempo de compilación el código que copia campos entre el DTO y la entidad:

```java
@Mapper(componentModel = "spring")
public interface ActivityMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    Activity toEntity(CreateActivityRequest request);

    @Mapping(source = "partner.name", target = "partnerName")
    ActivityResponse toResponse(Activity activity);

    List<ActivityCardResponse> toCardList(List<Activity> activities);
}
```

`componentModel = "spring"` hace que el mapper sea un bean. `ignore = true` para los campos que no vienen del DTO. `constant` fija el estado inicial. `source`/`target` conecta campos que se llaman distinto. Las listas se mapean solas.

**La trampa que os va a costar una mañana:** MapStruct y Lombok compiten por el orden de los procesadores de anotaciones. Si MapStruct va antes, no ve los _getters_ y genera un mapper vacío que compila pero no copia nada. **Ya está resuelto en el `pom.xml`** con `lombok-mapstruct-binding` en los `annotationProcessorPaths`; si alguna vez veis un mapper generado a medias, mirad ahí. Hay un ejemplo funcionando en `mapper/UserMapper.java`.

**Servicio y controlador.** `POST /api/admin/activities` y `PATCH /api/admin/activities/{id}/publish`. **Ninguna entidad sale por el controlador.**

> **Hecho cuando:** Se crea un borrador y una publicada desde Postman; con fechas incoherentes devuelve 400 con el campo señalado; con token de empleado, 403. Y el mapper de `target/generated-sources` tiene cuerpo de verdad.

#### `B2-03` Subida de la imagen de portada ⬜ · 1,0 d

`POST /api/admin/activity-images`, multipart con la parte `image`. `FileStorageService` valida tipo (**JPG o PNG, máximo 5 MB**), genera el nombre del archivo —**nunca el que envía el cliente**, que es por donde se cuelan rutas maliciosas—, lo guarda y devuelve `ImageUploadResponse { url }`. Esa URL es exactamente la que `CreateActivityRequest.imageUrl` espera.

`/uploads/**` **pide token**: no es una carpeta pública.

> **Hecho cuando:** Se sube una imagen y se recupera por su URL con token; un archivo de 20 MB se rechaza con 413; un GIF, con 415.

#### `B2-04` Revisión de PR y tablero ⬜ · 0,5 d

### Semana 3

#### `B2-05` Editar y cancelar actividad ⬜ · 1,5 d

**`GET /api/admin/activities/{id}`, el que carga el formulario.** Devuelve `ActivityFormResponse` con **todos** los campos que el formulario escribe y funciona con **cualquier estado, incluidos `DRAFT` y `CANCELLED`**. Solo `ADMIN`.

**Por qué una ruta aparte y no un `if` por rol dentro de la pública:** devuelven cosas distintas, y así la regla «los borradores son de administración» vive en la cadena de seguridad, donde se ve, y no dentro de un servicio, donde se olvida. Sin él **no se puede editar ninguna actividad**, porque el listado devuelve DTO de tabla y no trae los campos del formulario.

`PUT /api/admin/activities/{id}` con las mismas validaciones que la creación; editar una finalizada devuelve 409 `ACTIVITY_FINISHED`.

`PATCH /api/admin/activities/{id}/cancel` dentro de `@Transactional`: pasa la actividad a `CANCELLED` y **llama a `registrationLifecycleService.cancelAllForActivity(id)`**. Responde **204**.

**Tú no tocas `Registration`.** Nada de `setStatus` ni de una actualización masiva: qué estados pasan a cancelado es una regla de BE3. Llamas a la firma del contrato y ella decide. La llamada corre **dentro de tu transacción**: o se cancelan la actividad y sus inscripciones, o no se cancela ninguna de las dos cosas.

**El aviso va fuera.** `notificationService.notifyActivityCancelled(id)` se llama **después** de que el método transaccional vuelva, desde el controlador. Y no necesitas que BE3 haya escrito el correo: la implementación del día 1 solo deja traza, así que puedes terminar esto sin esperar a nadie.

> **Hecho cuando:** Cancelar deja la actividad fuera del catálogo y todas sus inscripciones canceladas, en una sola transacción.

#### `B2-06` Propuestas de entidades: modelo, endpoint público y semilla 🟡 · 1,5 d

**Lo hecho:** la entidad `Proposal` con su FK a `Partner` y a `Activity`, y `ProposalSeeder` con **`@Order(4)`**, seis propuestas por los tres estados.

**Lo que falta:** el endpoint y el servicio. `POST /api/proposals`, **público** — ya está declarado así en la cadena. `CreateProposalRequest` con `@NotBlank`, `@Email`, `@Size` y `@AssertTrue` en el consentimiento.

**Ojo con una cosa del modelo:** `Proposal` **no tiene campos de contacto**, porque apunta a un `Partner`. Así que el `CreateProposalRequest` público tiene que traer CIF y datos de contacto para **crear o localizar el `Partner`** por CIF, igual que hace el registro, y luego colgar la propuesta de él. Si no, una propuesta pública se queda sin nadie a quien responder.

El límite por IP lo pone `B1-22`.

> **Sin correo de acuse, de momento.** La v2 decía que se publicaba un evento para mandar un acuse de recibo. Ese método no existe entre los trece de `NotificationService`. Si se quiere, hay que ampliarlo por PR.

> **Hecho cuando:** Sin consentimiento devuelve 400 señalando la casilla; con datos válidos crea la propuesta en `NEW` colgada de su entidad; y un CIF que ya existe no crea una entidad duplicada.

#### `B2-07` Bandeja de propuestas, conversión y catálogo ⬜ · 1,75 d

**Tres estados y nada más:** una propuesta nace `NEW` y ahí se queda hasta que la administradora decide. **No hay estado «leída»**: abrirla no cambia nada, porque no aporta información que nadie vaya a usar.

`GET /api/admin/proposals` paginado y filtrado por estado. La respuesta lleva el estado, y con él el frontend decide qué botón enseñar: **Nueva** → «Crear actividad», **Aceptada** → «Ver actividad», **Rechazada** → ninguna acción.

`POST /api/admin/proposals/{id}/accept` crea la actividad en borrador con los datos precargados, guarda el `activity_id` en la propuesta y la marca `ACCEPTED`. Todo en una transacción. **Devuelve 201**, no 200, porque crea una actividad nueva. Aceptar una ya decidida devuelve 409 `PROPOSAL_ALREADY_DECIDED`.

`PATCH /api/admin/proposals/{id}/reject` la marca `REJECTED` y responde **204**.

**Catálogo.** `GET /api/activities` con plazas ocupadas y el booleano `favoritedByMe` — **sin el contador de «me gusta»**, que solo se sirve a la administradora — y `GET /api/activities/{id}` con `ActivityDetailResponse`.

> ⚠️ **El catálogo ya no es público.** Pide token de `EMPLOYEE` o `ADMIN`; una entidad colaboradora recibe **403** y lo suyo lo ve en `/api/org/activities`. La landing pública no lo necesita, porque sus cifras y líneas de acción son contenido estático. Y **no hay `/api/activities/published`**: es la misma ruta con filtros.

**Qué significa «visible», con los estados en la mano:** `PUBLISHED`, `FULL`, `IN_PROGRESS` y `FINISHED`. En cualquier otro estado, **404 y no 403**: quien no debe verla no debe ni saber que existe. **Ojo con `FULL`:** es un estado distinto de `PUBLISHED`, y si se queda fuera, la ficha deja de abrirse justo cuando se llena — que es cuando más gente entra a apuntarse a la cola.

Aquí es donde hay que vigilar el **N+1**: cargad el catálogo y contad las consultas.

> **Hecho cuando:** La bandeja se filtra por los tres estados; aceptar deja una actividad en borrador enlazada a su propuesta; y el catálogo se pinta con una consulta, no con trece.

#### `B2-08` Revisión de PR ⬜ · 0,25 d

#### `B2-13` Actividades de la entidad: listar, crear y editar ⬜ · 1,75 d

`GET /api/org/activities` con el estado y las plazas cubiertas. `POST` y `PUT`, con **las mismas validaciones que usa la administradora**, incluido `@ValidDateRange`.

**El filtro va por el usuario autenticado, nunca por parámetro.** Todo `/api/org/**` resuelve el `partnerId` desde el token. Un `?partnerId=3` que el servicio se creyera es el fallo de seguridad más típico de este tipo de plataforma, y se cuela con facilidad.

Editar solo se permite en `DRAFT`; en cualquier otro estado, 409 `ACTIVITY_NOT_EDITABLE`. La entidad **no** puede fijar el estado: nace en `DRAFT` y de ahí solo la mueve `submit`.

> **Hecho cuando:** La entidad ve solo sus actividades · Pasar el identificador de otra por parámetro no devuelve nada suyo · Editar una ya enviada devuelve 409 · Las validaciones de fecha se comportan igual que en el formulario de la administradora.

#### `B2-14` Enviar a revisión y bloqueo de edición ⬜ · 0,5 d

`PATCH /api/org/activities/{id}/submit` pasa de `DRAFT` a `PENDING_APPROVAL` y llama a `notifyActivitySubmittedForReview`.

**`DRAFT` y `PENDING_APPROVAL` no son lo mismo** y por eso son dos estados: el primero significa «se está escribiendo» y el segundo «está terminada y espera a alguien». Van a bandejas distintas.

Antes de aceptar comprueba que están los campos obligatorios. Desde `PENDING_APPROVAL` la entidad ya no edita.

#### `B2-15` Aprobar o devolver una actividad propuesta ⬜ · 1,0 d

`GET /api/admin/activities/pending` ordenado por antigüedad.

`PATCH /api/admin/activities/{id}/approve` la pasa a `PUBLISHED` y llama a `notifyActivityApproved`. La administradora **puede editarla antes de aprobar** con el `PUT` que ya existe.

`PATCH /api/admin/activities/{id}/return` **exige comentario** —`@NotBlank`, y sin él 400—, lo guarda en `reviewNote` y devuelve la actividad a `DRAFT`. Llama a `notifyActivityReturned`, que lleva el comentario dentro.

**`approve` y `return` son solo para actividades propuestas por una entidad. Los cierres no se devuelven.**

> **Hecho cuando:** Aprobar publica y aparece en el catálogo · Devolver sin comentario devuelve 400 · Una actividad devuelta vuelve a ser editable y conserva el comentario · Aprobar o devolver algo que no está en revisión devuelve 409.

#### `B2-16` Proponer desde dentro ⬜ · 0,5 d

`POST /api/org/proposals` y `GET /api/org/proposals`. La propuesta se crea con el `Partner` resuelto desde el token, así que el cuerpo **solo lleva la necesidad y los voluntarios estimados**: la entidad no reescribe su CIF ni su contacto.

**Las dos vías acaban en la misma tabla y en la misma bandeja.** La pública crea o localiza el `Partner` por CIF; esta lo tiene ya. Si se dejan como dos caminos separados, la administradora acaba con dos bandejas y ninguna cuadra.

### Semana 4

#### `B2-09` Bloque de pruebas · ver la sección 9 ⬜ · 2,0 d

#### `B2-10` README del catálogo, diagrama ER y Postman ⬜ · 1,0 d

Documentar las tablas de actividad y propuesta, sus estados y sus transiciones. Subir el diagrama entidad-relación de `C-02`, actualizado a las **ocho** tablas reales.

#### `B2-11` Corrección de errores y ensayo de tu parte de la demo ⬜ · 1,0 d

Guion: llega una propuesta por el formulario público, la administradora la convierte en actividad y la publica.

#### `B2-17` Pruebas del rol entidad · BE2 ⬜ · 0,5 d

Unitarias de las transiciones: `DRAFT → PENDING_APPROVAL → PUBLISHED` y la vuelta con devolución. Integración: **aislamiento entre entidades** — con la sesión de Cáritas, pedir las actividades de otra, por identificador y por parámetro, no devuelve nada.

---

## BE3 · Participación: inscripciones, cupo, cola y correo

La mecánica que define el producto y el canal que avisa a la gente. Cubre H8 · H9 · H12 · H13 · H14 · H15 · H23 · H24.

**Es dueña de:** `Registration` · `Favorite` · `SpotService` · `RegistrationService` · `RegistrationLifecycleService` · `FavoriteService` · `NotificationService`

### Semana 1

#### `B3-01` Entidad Registration, proyecciones y semilla ✅ · 1,25 d

Hecha. La entidad con `status`, `accepted`, `queuePosition`, `decidedBy` y `decidedAt`, **sin campo de motivo de rechazo**: el rechazo es un estado y no lleva texto asociado. `RegistrationSeeder` con **`@Order(5)`**, con inscripciones por todos los estados, una actividad llena con cola, y varias en `PENDING_CLOSURE` — que son las que `ClosureSeeder` de BE1 necesita.

Y las **dos** firmas del contrato que consumen BE1 y BE2: `countByActivityIdAndStatus` y `findByActivityIdAndStatusOrderByQueuePosition`.

> **`findClosedForDashboard` ya no es tuya.** La v2 la ponía aquí; vive en `ParticipationClosureRepository` y la escribió BE1. Es una entrega menos.

**Los seis estados y qué significa cada uno:** `WAITLISTED` (solicitada, sin plaza) · `CONFIRMED` (tiene plaza) · `REJECTED` (la admin dijo que no) · `CANCELLED` (la canceló la persona, o la dio de baja la admin) · `PENDING_CLOSURE` (la actividad terminó y falta el cierre) · `CLOSED` (la Fundación cerró la actividad).

**Quién escribe cada uno:** `WAITLISTED` y `CONFIRMED`, `SpotService`; `REJECTED` y `CANCELLED`, la decisión del admin o de la persona; `PENDING_CLOSURE`, la tarea programada de `B3-17`; y `CLOSED`, `closeAllForActivity`. **Si un estado no tiene quién lo escriba, ese camino no existe** — es exactamente lo que pasaba con `FINISHED` antes de `B3-17`.

**La regla de no repetir inscripción vive aquí:** `existsByActivityIdAndUserIdAndStatusNot(activityId, userId, CANCELLED)`, llamada desde `SpotService` antes de insertar. Traducido: puedes volver a apuntarte si cancelaste, no puedes si te rechazaron o si ya estás dentro. **Falta añadirla al repositorio**, es lo único que le queda a esta tarea.

### Semana 2

#### `B3-02` SpotService.register: cupo, no repetir y fecha límite ⬜ · 1,5 d

Método `@Transactional`. Comprueba la fecha límite (`DEADLINE_PASSED`), comprueba que no tenga ya una inscripción que bloquee (`ALREADY_REGISTERED`) y crea la inscripción en `WAITLISTED` con posición al final de la cola. El cupo y la fecha los lee con **`findSpotInfo`**, que BE2 ya tiene escrita y funcionando.

Excepciones de dominio propias con sus códigos, traducidas por el `GlobalExceptionHandler` que ya existe: lanza `DomainException` con el `ErrorCode` que toque y el manejador hace el resto.

También te toca poner la actividad en **`FULL`** cuando se cubre la última plaza.

> **Hecho cuando:** Solicitar dos veces devuelve 409 con su código; solicitar fuera de plazo, 400 con el suyo.

#### `B3-03` Controlador de inscripciones y decisión del admin ⬜ · 1,5 d

`POST /api/registrations` y `GET /api/admin/registrations?activityId=` con la proyección de persona, departamento, organización y horas del año, más los contadores de confirmadas, en cola y sin revisar.

`PATCH /api/registrations/{id}/accept` dentro de `@Transactional`: marca `accepted = true`; si quedan plazas confirma en el acto, y si no, deja a la persona en la cola con su posición.

**Aceptar no falla nunca por aforo, y es deliberado.** Inscribirse crea siempre en `WAITLISTED` y aceptar con el cupo lleno deja la marca de apta, así que **ninguna operación de este proyecto puede responder «actividad llena»**. Por eso **no existe** `ACTIVITY_FULL`: un código que nadie puede lanzar hace que frontend escriba un mensaje y una rama para algo que no ocurre.

**Y con el aforo lleno no hay «dar plaza ahora»:** si no hay hueco, no hay plaza que dar. La única forma de que alguien de la cola entre es que se libere una, y de eso se encarga `promoteFirstInQueue`. El prototipo tenía ese botón y se ha quitado.

`PATCH /api/registrations/{id}/reject` pasa a `REJECTED`, **sin cuerpo y sin motivo**: no hace falta DTO de petición.

Las dos llaman a su `notifyXxx` **fuera de la transacción** y registran quién decidió y cuándo.

> **Estas dos rutas son de `ADMIN` y no tienen prefijo que lo imponga.** Ponles `@PreAuthorize("hasRole('ADMIN')")`, porque la cadena solo exige token.

#### `B3-04` Revisión de PR y tablero ⬜ · 0,5 d

#### `B3-13` Correos del ciclo de la cuenta ⬜ · 0,5 d

Tres plantillas sobre la base de `B3-09`: **verificación** (con el enlace y su caducidad), **cuenta aprobada** (con el enlace al panel) y **cuenta rechazada** (sin motivo detallado, igual que el rechazo de inscripción). Corresponden a `notifyVerificationRequested`, `notifyOrgAccountApproved` y `notifyOrgAccountRejected`.

**El de verificación es el único que no puede fallar en silencio:** si no llega, la entidad se queda bloqueada sin saber por qué. Registrar el fallo con nivel de aviso, no de información.
_Depende de `B1-16`._

### Semana 3

#### `B3-05` promoteFirstInQueue con bloqueo pesimista ⬜ · 1,5 d

El método más delicado del proyecto. Al liberarse una plaza, busca la primera persona **apta** por `queuePosition`, la confirma, reordena la cola y llama a `notifySpotReleased` con **el identificador de la inscripción que ascendió**, no el de la que se canceló.

Todo dentro de una transacción con `@Lock(PESSIMISTIC_WRITE)` sobre la actividad: sin ese bloqueo, dos bajas simultáneas ascienden a dos personas a la misma plaza y no os enteraréis hasta que alguien se presente y no tenga sitio.

> **Hecho cuando:** Dos bajas lanzadas a la vez producen exactamente un ascenso.

#### `B3-06` Cancelar una inscripción, desde los dos lados ⬜ · 1,5 d

**Un solo endpoint, no dos.** `PATCH /api/registrations/{id}/cancel`, el mismo para el empleado y para la administradora. No hay `DELETE`: **no se borra nada**, la fila se queda en `CANCELLED`, porque hace falta para saber quién estuvo, para la regla de no repetir inscripción y para que el dashboard no pierda historia. Un `DELETE` que no borra es una mentira en la API.

Quien decide qué se permite es el servicio, mirando quién llama:

```
si quien llama es la dueña de la inscripción
  → solo hasta la fecha de inicio, si no 409
si quien llama tiene rol ADMIN
  → siempre, con motivo opcional
en cualquier otro caso → 403 NOT_OWNER
```

A partir de ahí las dos vías hacen lo mismo: pasan a `CANCELLED`, liberan la plaza y llaman a `promoteFirstInQueue`.

**El servicio tiene que devolver a quién ascendió.** El destinatario de `notifySpotReleased` nace dentro de la transacción y su identificador no está en la ruta, así que el controlador no puede adivinarlo:

```java
public record CancelResult(RegistrationResponse body, Long promotedRegistrationId) {}
```

Si `promotedRegistrationId` viene a `null`, no ascendió nadie y no hay aviso que mandar.

**Además, `GET /api/registrations/me`**, que devuelve `List<MyRegistrationItem>`:

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

**Esos dos últimos campos son imprescindibles.** El empleado llega a «Mis voluntariados» con un `registrationId` y nada más, y con ellos la pantalla decide sola:

| `closureId` | `activityClosed` | Botón |
|---|---|---|
| `null` | `false` | «Cerrar tu participación» |
| tiene valor | `false` | «Cierre enviado» · solo lectura |
| tiene valor | `true` | «Descargar certificado» |

El `closureId` lo lees con `ParticipationClosureRepository.findByRegistrationId`, que ya está escrita.

**Y aquí viven las dos operaciones en masa de `RegistrationLifecycleService`**, cuya implementación hoy devuelve `0`:

```
cancelAllForActivity(activityId)  // la llama BE2 al cancelar
closeAllForActivity(activityId)   // la llama BE1 al finalizar el cierre
```

Las dos devuelven el número de filas afectadas y corren **dentro de la transacción de quien las llama**. `closeAllForActivity` pasa de `PENDING_CLOSURE` a `CLOSED`, que es la transición que mete las horas en los dos dashboards: por eso es tuya aunque la dispare BE1.

> **Hecho cuando:** El empleado cancela antes del inicio y funciona; después, 409. La administradora cancela cuando quiera. En los dos casos la plaza se libera y asciende la primera aceptada. En la base de datos no desaparece ninguna fila. Y las dos operaciones en masa dejan todas las filas de la actividad en el estado correcto de una pasada.

#### `B3-07` «Me gusta» y su semilla 🟡 · 0,75 d

**Lo hecho:** la entidad `Favorite` con su `@UniqueConstraint` sobre `(activity_id, user_id)` y `FavoriteSeeder` con **`@Order(6)`**, con «me gusta» repartidos de forma desigual para que el ranking del dashboard tenga forma.

**Lo que falta:** `POST /api/favorites` con `FavoriteRequest { activityId }` y `DELETE /api/favorites/{activityId}`. **Ojo con las rutas**, que no son las que decía la v2: cuelgan de `/api/favorites`, no de `/api/activities/{id}/favorite`.

**Dónde se ve el número y dónde no.** El empleado **nunca ve el contador**: en el catálogo y en la ficha el corazón solo aparece marcado o sin marcar. `ActivityCardResponse` y `ActivityDetailResponse` llevan un booleano `favoritedByMe` y **no** llevan `favoriteCount`. El recuento existe, pero solo lo consumen el dashboard y el listado de la administradora.

No es un capricho: enseñar «3 me gusta» hace que una actividad parezca poco interesante y condiciona a quien la mira. Sirviendo el contador solo a administración, el dato cumple su función —saber qué interesa aunque no se llene— sin efecto rebaño.

#### `B3-08` Revisión de PR ⬜ · 0,5 d

Esta semana BE1 escribe el dashboard: comprueba que solo cuenta actividades cerradas.

#### `B3-17` Paso automático a IN_PROGRESS, FINISHED y PENDING_CLOSURE ⬜ · 0,5 d

**Sin esta tarea, nadie puede cerrar nada.** Todo el ciclo de cierre arranca cuando una actividad llega a `FINISHED`: `B1-03` rechaza con `ACTIVITY_NOT_FINISHED` y la cola de `B1-04` lista «actividades finalizadas». **Ninguna otra tarea escribe ese estado** — solo lo leen.

Tarea programada diaria de madrugada con `@Scheduled`:

```
PUBLISHED · FULL      → IN_PROGRESS   al llegar start_date
IN_PROGRESS           → FINISHED      al pasar end_date
y de cada actividad que pasa a FINISHED:
  sus inscripciones CONFIRMED → PENDING_CLOSURE
```

Llama a `notifyActivityFinished(id)` **después** de cerrar la transacción de cada actividad. **El método `@Scheduled` no lleva `@Transactional`**: él orquesta, y la transacción la abre el servicio al que llama.

**Y un endpoint de administración para dispararla a mano:** `POST /api/admin/activities/refresh-status`. Sin él, la demo depende de esperar a medianoche. **No está en el contrato de API**: hay que añadirlo por PR antes de implementarlo.

**Idempotente:** ejecutarla dos veces el mismo día no puede duplicar transiciones ni volver a avisar.

> **Hecho cuando:** Con una actividad cuya fecha de fin ya pasó, una ejecución la deja en `FINISHED` y sus inscripciones confirmadas en `PENDING_CLOSURE`; una segunda ejecución no cambia nada y no manda ningún correo nuevo.

#### `B3-14` Correos de actividad aprobada y devuelta ⬜ · 0,25 d

Dos plantillas más. La de **devuelta lleva el `reviewNote` en el cuerpo**, no un «entra a verlo»: si la entidad tiene que abrir la plataforma para saber qué corregir, la mitad no lo hace. La de aprobada lleva el enlace a la actividad publicada.

### Semana 4

#### `B3-09` NotificationServiceImpl: plantillas y envío asíncrono ⬜ · 1,5 d

**Rellenar la implementación que ya está subida.** La interfaz no se toca: los trece métodos están y las otras dos llevan semanas llamándolos. Aquí solo cambia lo que hay dentro de `NotificationServiceImpl` y de `MailDispatcher`.

**Lo primero: añadir `spring-boot-starter-thymeleaf` al `pom.xml`.** No está, y todo lo demás depende de ello.

**Configuración** del buzón de pruebas con las credenciales en `.env`. `@EnableAsync` **ya está** en `config/AsyncConfig`, y `MailDispatcher` **ya es un bean aparte con `@Async`**, que es lo que hace que la anotación no se ignore.

**Una plantilla base** con la cabecera coral, el logotipo y el pie legal, y una plantilla por correo que solo cambie el cuerpo, con fragmentos de Thymeleaf. Trece correos sin repetir el diseño trece veces.

**Los enlaces son el único punto donde el correo toca al frontend:** las rutas las entrega FE2 —`/my-volunteering`, `/activities/{id}`, `/closures/{id}`— y se montan sobre **`app.base-url`**, que ya está en `application.properties`. **Ninguna plantilla lleva un dominio fijo.** Si una ruta cambia, se toca un valor y no trece archivos.

**El envío nunca tumba la operación.** Dentro del método, capturad la excepción de correo, dejadla en la traza y seguid. Que el buzón esté caído no puede impedir que alguien consiga su plaza. Y con `@Async` esto es doblemente importante: **una excepción en un método asíncrono no llega a quien lo llamó**, se pierde, y sin el `catch` no queda ni rastro.

> **Hecho cuando:** Los trece llegan al buzón con su diseño; con el servidor de correo caído la aplicación sigue respondiendo y la operación de negocio se completa; y una operación que se deshace no manda ningún correo.

#### `B3-10` Bloque de pruebas · ver la sección 9 ⬜ · 2,5 d

#### `B3-11` Documentación de la API y README de participación ⬜ · 0,5 d

Springdoc sobre todos los controladores, coordinado con BE1 y BE2. README con la máquina de estados de la inscripción, qué significa la marca de apta y por qué existe el bloqueo pesimista.

#### `B3-12` Corrección de errores y ensayo de tu parte de la demo ⬜ · 0,5 d

Guion: un empleado solicita plaza con el aforo lleno, entra en la cola, la administradora da de baja a alguien y la plaza asciende sola. Es el momento más vendible de la demo.

#### `B3-15` Datos de demostración del rol entidad 🟡 · 0,1 d

**Casi hecha, y no como se planificó.** La v2 pedía un `OrgUserSeeder` con `@Order(8)`. No hace falta: `UserSeeder` ya siembra cuentas con rol `PARTNER` en los cuatro `UserStatus`, `PartnerSeeder` cubre los tres estados de entidad, y `ActivitySeeder` tiene una actividad en `DRAFT` y otra en `PENDING_APPROVAL`.

**Lo que queda es comprobar dos cosas** al arrancar en limpio: que se puede entrar con una cuenta de entidad activa, y que hay **una actividad devuelta con su `reviewNote` relleno** para poder enseñar ese caso. Si falta, se añade a `ActivitySeeder` — que es de BE2, así que se pide por el canal.

#### `B3-16` Pruebas del rol entidad · BE3 ⬜ · 0,5 d

Que los cinco correos nuevos se disparan por su método de `NotificationService` y **solo después de que la transacción confirme**. Que un fallo del servidor de correo no tumba la operación de negocio.

---

## 9 · El bloque de pruebas

Todas las pruebas van juntas en la semana 4, cuando los endpoints ya no van a cambiar. Escribir tests contra código que todavía se mueve es tirar el tiempo dos veces. A cambio, hay que asumir una cosa: **si algo se rompe antes de la semana 4, os enteraréis probando a mano.**

**P1 se escribe sí o sí.** P2 se escribe si la semana 4 va según lo previsto. P3 solo si sobra tiempo, que no va a sobrar. La rúbrica pide unitarios de front, unitarios de back e integrados de back: con P1 y P2 los tres apartados quedan cubiertos.

### P1 · Imprescindibles · 3,25 días-persona

| Quién | Tipo | Qué prueba | Casos | Días |
|---|---|---|---|---|
| **BE3** | Unitario | `SpotService`: los ocho casos del cupo | Confirma con hueco · encola sin hueco · rechaza duplicado · rechaza fuera de plazo · asciende a la primera apta · **no asciende a una sin revisar** · reordena la cola tras el ascenso · dos bajas concurrentes producen un solo ascenso. | 1,25 |
| **BE3** | Integración | Ciclo completo de inscripción | `@SpringBootTest` con MockMvc: solicitar, aceptar con aforo lleno, dar de baja a una confirmada y comprobar el ascenso automático. | 0,75 |
| **BE1** | Integración | Autenticación y autorización por rol | Login correcto devuelve token · login incorrecto, 401 · empleado en endpoint de admin, 403 · token ausente, 401 · exportación 200 para admin y 403 para empleado. | 0,75 |
| **BE1** | Unitario | `ParticipationClosureService` y `ActivityClosureService` | Cerrar una actividad sin terminar falla con `ACTIVITY_NOT_FINISHED` · cerrar una inscripción no confirmada falla con `REGISTRATION_NOT_CONFIRMED` · **`finalize` pasa todas las inscripciones a `CLOSED` de una vez** · finalizar dos veces devuelve 409. | 0,5 |

> **El caso de prueba de la v2 «validar fija las horas» ya no existe.** No hay validación de horas: las que declara el empleado son las definitivas. Se ha sustituido por el caso de `finalize`, que es la transición que de verdad sostiene el dashboard.

### P2 · Importantes · 2,75 días-persona

| Quién | Tipo | Qué prueba | Casos | Días |
|---|---|---|---|---|
| **BE2** | Unitario | `ActivityService` y `ProposalService` | Fechas incoherentes · editar una finalizada · cancelar en cascada · propuesta sin consentimiento · conversión que precarga los campos correctos · aceptar una propuesta ya decidida. | 1 |
| **BE2** | Integración | Ciclo de la actividad y formulario público | Crear borrador, publicar, editar, cancelar y comprobar que las inscripciones quedan canceladas. Formulario público con campos vacíos devuelve 400 con el mapa de campos. | 1 |
| **BE1** | Unitario | `JwtService` y `CsvExportService` | Token caducado y token manipulado se rechazan · la salida del CSV no contiene nombre ni correo. | 0,75 |

### P3 · Si sobra tiempo · 1 día-persona

| Quién | Tipo | Qué prueba | Casos | Días |
|---|---|---|---|---|
| **BE1** | Unitario | `CertificateService` | Propiedad ajena, 403 · actividad sin cerrar, 409 · la referencia no cambia entre llamadas. | 0,5 |
| **BE3** | Unitario | `FavoriteService` y `NotificationService` | Unicidad del «me gusta» · el fallo de envío de correo no tumba la operación. | 0,5 |

---

## 10 · Puntos de sincronización

| Cuándo | Quién | Qué | Para qué |
|---|---|---|---|
| Días 1 a 3 | Las tres | **Las tareas conjuntas** | Arranque, tablas, contrato de código y API, y seguridad. **Ya están hechas.** |
| Cada mañana · 10 min | Las tres | **Punto de sincronización** | Qué toqué ayer, qué toco hoy, qué necesito de alguien. |
| Final de la semana 2 · 2 h | Las tres | **Primera integración cruzada** | Levantar el proyecto con las tres ramas fusionadas y recorrer con Postman: login, crear actividad, publicarla, solicitar plaza, aceptar. |
| Final de la semana 3 · 2 h | Las tres | **Recorrido completo de punta a punta** | El bucle entero: solicitar, aceptar, dar de baja, comprobar el ascenso, cerrar y ver el dato en el dashboard. **Si esto funciona, la semana 4 es solo pruebas y acabado.** Si no, es rescate y los tests P2 se caen. |
| Semana 4 | Todo el equipo | **Ensayo de la demo** | Dos pases cronometrados con los seis. |

### Qué hacer si te bloqueas

| Situación | Qué hacer |
|---|---|
| **Necesito leer datos de un dominio que no es mío** | Usa el repositorio del contrato. Si el método no existe, pídelo por el canal: su dueña lo añade en el día. Mientras tanto escribe tu servicio contra la interfaz y simúlala en tus tests. |
| **Necesito avisar de algo que pasa en mi dominio** | Llama al método `notifyXxx` que corresponda, **desde el controlador, después de que el servicio haya vuelto**. No hay eventos de Spring en este proyecto, y no llames tú a `MailDispatcher`. Funciona aunque las plantillas no estén escritas: hasta la semana 4 solo deja traza. |
| **Necesito cambiar el estado de una entidad que no es mía** | Solo hay dos formas y las dos están en el contrato: `cancelAllForActivity` y `closeAllForActivity`. Si necesitas una tercera, es una conversación, no un `setStatus`. |
| **La tabla de otra persona no existe** | No debería pasar: las ocho entidades están en `dev` y Hibernate crea las ocho tablas al arrancar. Si te pasa, casi seguro tienes la base de datos vieja: bórrala y vuelve a arrancar. |
| **Hibernate se queja de una columna que ya no existe en la entidad** | Es `ddl-auto=update`, que añade pero no borra. Borra la base de datos y arranca otra vez. Si pasa a menudo, avisad en el punto de la mañana. |
| **Necesito un token real para probar** | Entra con una cuenta de `UserSeeder` — el login funciona desde C-04 — o usa `@WithMockUser` en los tests. |
| **Mi tarea está terminada y me sobra tiempo** | Adelanta tus tests P1. Nunca empieces la tarea de otra persona sin decírselo. |
| **Estoy atascada más de media jornada** | Dilo en el punto de la mañana siguiente, o antes. En un proyecto de 20 jornadas, medio día perdido es el 2,5% de tu tiempo; dos días es el 10%. |

---

## 11 · Enmiendas pendientes del contrato de API

Cuatro puntos en los que **este documento y `docs/api-contract.md` no dicen lo mismo**. No se resuelven a solas: cada uno necesita un PR sobre el contrato, revisado por las tres, y algunos afectan a frontend.

| # | Qué | Dónde choca |
|---|---|---|
| 1 | **`POST /api/admin/activities/refresh-status`** no está entre los 52 endpoints del contrato, pero `B3-17` lo necesita para poder enseñar la demo sin esperar a medianoche. Añadirlo o quitarlo de la tarea. | `B3-17` |
| 2 | **Corregir un cierre de participación.** `B1-03` dice que un segundo `POST /api/closures` actualiza y responde 200; el contrato solo documenta 201 y `CLOSURE_ALREADY_CLOSED`. Son dos comportamientos distintos para el mismo endpoint. | `B1-03` · contrato §6.7 |
| 3 | **Campos de requisitos en `Activity`.** `B2-05` describe un formulario con «dirección y requisitos»; la entidad solo tiene `location`. O se añade la columna o se quita de la pantalla. | `B2-05` · `C-02` |
| 4 | **Acuse de recibo de propuesta.** No hay método en `NotificationService` para avisar a quien envía el formulario público. Si se quiere, son catorce métodos y no trece. | `B2-06` · contrato §5 |

Y los seis puntos que `docs/api-contract.md` marca con ⚠️ siguen **pendientes de repasar con las tres personas de frontend**. El más urgente es el sexto: los `record` de `dto/closure/` y `dto/activityclosure/` están vacíos, y hasta que tengan campos frontend no puede mockear ninguna pantalla de cierre.

---

## 12 · Diez avisos

**Sin migraciones, las entidades son el esquema: no se tocan a la ligera.** `ddl-auto=update` añade pero **nunca borra ni renombra**. Renombrar un campo deja la columna vieja ahí para siempre. La solución cuando la base de datos se ponga rara es borrarla y volver a arrancar: las semillas la rellenan en segundos.

**No hay constraint de unicidad en la tabla de inscripciones, y es a propósito.** La regla es «no puedes volver a apuntarte salvo que cancelaras tú», y eso permite varias filas de la misma persona en la misma actividad. Por eso la regla vive en `SpotService`, en una línea, y tiene su test en P1: es de las cosas que se rompen sin hacer ruido.

**El rol se llama `PARTNER` y las rutas `/api/org/**`.** Es la incoherencia que más veces se ha colado en los documentos. Si veis `Role.ORG` o `@WithMockUser(roles="ORG")` en cualquier sitio, está mal.

**El motivo de rechazo no existe, y el prototipo todavía lo pide.** La ventana de «Rechazar solicitud» tiene un desplegable de motivo. Si el backend no lo guarda, hay que quitarlo de la pantalla y del correo, o volver a meterlo en la entidad. Decidid una de las dos, pero que la pantalla y la tabla digan lo mismo.

**Con 20 jornadas no hay colchón.** El orden de recorte, si algo se tuerce: el PDF con gráficos (`B1-10`), los tests P3, y después los P2. La funcionalidad no se toca.

**Dejar los tests para el final tiene un precio.** Hasta la semana 4 no hay red. Por eso los dos recorridos con Postman de las semanas 2 y 3 **no son opcionales**.

**El bloqueo pesimista de `promoteFirstInQueue` es lo único que puede fallar en silencio.** Y es también lo que un tribunal puede preguntar y dejar en evidencia. Es el único sitio del código donde un comentario está justificado: explicad **por qué** existe el bloqueo, no qué hace.

**Dos de las 24 historias son de un solo lado.** **H3 (landing pública) no tiene backend:** sus cifras son estáticas. **H23 (recibir correos) no tiene frontend:** toda la historia vive en `B3-09`. En el tablero conviene marcarlas, para que la ausencia se lea como una decisión y no como un olvido.

**El dashboard solo cuenta participaciones cerradas, y ahí se cruzan dos seeders.** Los cierres los crea `ClosureSeeder` (BE1) sobre las inscripciones que dejó `RegistrationSeeder` (BE3). Si BE3 no deja inscripciones en `PENDING_CLOSURE`, o si BE1 no escribe su seeder, el dashboard sale a cero y la parte más vendible de la demo se queda en blanco. Comprobadlo en la integración de la semana 3, no el día de la defensa.

**El contrato de API vive en un solo archivo y se cambia por PR.** Nunca en un mensaje de chat, y nunca copiándolo a otro documento. Esta v3 borró la copia que había aquí precisamente porque se había separado del original sin que nadie lo notara.
