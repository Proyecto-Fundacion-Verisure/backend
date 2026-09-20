# C-04 + C-06 · Seguridad: JWT, Spring Security, CORS y tres roles

**Plan aprobado punto por punto · 4 de septiembre de 2026**

## Por qué

`spring-boot-starter-security` está en el `pom.xml` **sin ninguna configuración**. Cuando eso ocurre, Spring Boot aplica su cadena por defecto y protege todos los endpoints con autenticación básica y una contraseña generada en cada arranque. La aplicación levanta, pero **frontend no puede llamar a nada**.

Las dos issues (#121 y #157) van juntas porque C-06 no es una tarea aparte: es «lo mismo, pero con el tercer rol». Separarlas obligaría a escribir la cadena dos veces.

## Decisiones

| | |
|---|---|
| Rol | **`PARTNER`** en todo el proyecto, nunca `ORG` |
| Estructura | La de proyectos anteriores: `security/filter/` con los dos filtros, más `CustomAuthenticationManager`, `UserDetail` y `SpringConfig` |
| Login | **Entra en C-04**, dentro de `JWTAuthentication`. Invade #120 (B1-02), que se queda con `/logout` y `/me` |
| Token | Clase aparte, `JwtService`, para no duplicar la lógica en los dos filtros |
| Estados de cuenta | **C-04 distingue el motivo** (`ACCOUNT_NOT_VERIFIED`, `ACCOUNT_PENDING_APPROVAL`, `ACCOUNT_REJECTED`). Invade también B1-17 |
| Contraseña de demostración | La misma para las 19 cuentas: `Verisure2026!` |
| `/uploads/**` | Con token |
| Catálogo | **Ya no es público**: `EMPLOYEE` y `ADMIN` |
| Tests | **Aplazados al final de todo el proyecto** |
| Perfil `test` con base aislada | Sigue aplazado |
| Límite por IP | No entra · queda como `TODO` para B1-14 y B1-15 |

---

## 1 · `security/SpringConfig.java` · la cadena

Se rellena **en su sitio**, sin renombrar a `SecurityConfig`.

`@Configuration @EnableWebSecurity @EnableMethodSecurity`, `SessionCreationPolicy.STATELESS`, CSRF desactivado —con JWT en una cabecera no hay cookie que falsificar—, los dos filtros y los manejadores de error del punto 6.

> **Desviación al implementar:** el `BCryptPasswordEncoder` **no** puede vivir aquí. `SpringConfig` necesita `CustomAuthenticationManager` para montar el filtro de login, y el manager necesita el codificador: Spring detecta la dependencia circular y **el contexto no arranca**. Está en su propia clase, `security/PasswordEncoderConfig.java`.

## 2 · `security/JwtService.java`

**Con `java-jwt` de Auth0 `4.5.2`**, que es lo que hay en el `pom.xml`. El runbook da los ejemplos con `jjwt 0.12.5`: quien copie ese código importará clases que no existen.

```java
public String generateToken(String email, Role role);   // lo usa JWTAuthentication
public Optional<DecodedJWT> validate(String token);     // lo usa JWTAuthorization
```

No consulta la base de datos. Caducidad de **2 horas**, que es la política de revocación completa, porque `logout` no revoca nada.

`validate` devuelve `Optional` en vez de lanzar: un token caducado es el caso normal a las dos horas, no un error, y así el filtro lo trata igual que la ausencia de token, sin excepciones dentro de la cadena.

## 3 · `security/UserDetail.java`

Envuelve la entidad `User` en un `UserDetails`.

> ⚠️ **`hasRole` añade el prefijo `ROLE_` por su cuenta.** Si la autoridad se expone como `PARTNER` a secas, `hasRole("PARTNER")` busca `ROLE_PARTNER`, no coincide nunca, y **todo devuelve 403** sin ningún error que lo explique. Las autoridades se construyen como `ROLE_ADMIN`, `ROLE_EMPLOYEE` y `ROLE_PARTNER`.

`isEnabled()` devuelve cierto solo para `UserStatus.ACTIVE`. De las 19 cuentas sembradas, **tres no lo están** y no podrán entrar, que es lo correcto.

## 4 · `security/CustomAuthenticationManager.java`

```java
User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BadCredentialsException("Credenciales no válidas"));

if (!passwordEncoder.matches(password, user.getPassword())) {
    throw new BadCredentialsException("Credenciales no válidas");   // 401 genérico
}

switch (user.getStatus()) {
    case PENDING_VERIFICATION -> throw new AccountStatusException(ACCOUNT_NOT_VERIFIED);
    case PENDING_APPROVAL     -> throw new AccountStatusException(ACCOUNT_PENDING_APPROVAL);
    case REJECTED             -> throw new AccountStatusException(ACCOUNT_REJECTED);
    case ACTIVE               -> { }
}
```

> ⚠️ **El orden importa.** Si el estado se comprobara **antes** que la contraseña, cualquiera podría escribir correos al azar y averiguar cuáles tienen cuenta y en qué estado, sin conocer ninguna contraseña. Comprobando primero la contraseña, esa información solo la ve quien ya ha demostrado ser esa persona.

Hace falta **`AccountStatusException extends AuthenticationException`** llevando su `ErrorCode` dentro, igual que `DomainException`.

**Dos piezas que hoy no existen:** `UserRepository.findByEmail` —la interfaz está vacía— y **cifrar las contraseñas del seeder**, que hoy son el marcador `TODO-C04-sin-cifrar`, así que ninguna cuenta puede entrar.

## 5 · Los dos filtros · `security/filter/`

**`JWTAuthentication`** extiende `UsernamePasswordAuthenticationFilter` y **es** el login:

```java
setFilterProcessesUrl("/api/auth/login");

attemptAuthentication(...)       // lee el JSON y delega en CustomAuthenticationManager
successfulAuthentication(...)    // jwtService.generateToken(...) y escribe AuthResponse
unsuccessfulAuthentication(...)  // ApiError: 401 genérico, o 403 con el estado de la cuenta
```

> ⚠️ **`UsernamePasswordAuthenticationFilter` viene preparada para formularios HTML** y lee `username` y `password` como parámetros de formulario. El contrato manda **JSON** con `email` y `password`. Hay que sobrescribir `attemptAuthentication` para deserializar el cuerpo con `ObjectMapper`; si no, el login recibe `null` en las credenciales y falla siempre sin decir por qué.

Devuelve `AuthResponse { accessToken, tokenType: "Bearer", expiresIn: 7200, user }`, lo que obliga a rellenar `dto/user/UserResponse`, hoy un *record* vacío.

**`JWTAuthorization`** se ejecuta en cada petición: lee `Authorization: Bearer …`, valida con `JwtService` y puebla el `SecurityContext` **con el rol que viaja dentro del token**, sin consultar la base de datos.

> ⚠️ **Nunca lanza.** Las rutas públicas pasan también por aquí; si reventara con una petición sin cabecera, el formulario público de propuestas dejaría de funcionar.
>
> Contrapartida de sacar el rol del token: si a alguien se le rechaza la cuenta, su token sigue valiendo hasta caducar, como máximo 2 horas.

## 6 · Que el 401 y el 403 devuelvan `ApiError`

**Es el punto que más fácilmente se queda a medias.** `GlobalExceptionHandler` tiene `@ExceptionHandler` para `AuthenticationException` y `AccessDeniedException`, y **aun así no funcionará**: Spring Security las lanza **dentro de la cadena de filtros**, antes del `DispatcherServlet`, y un `@RestControllerAdvice` solo ve lo que nace dentro de un controlador.

| Quién responde | Cuándo | Código |
|---|---|---|
| `JWTAuthentication.unsuccessfulAuthentication` | Falla el **login** | 401 genérico, o 403 con el estado |
| `RestAuthenticationEntryPoint` | Ruta protegida sin token, o inválido o caducado | **401** `UNAUTHORIZED` |
| `RestAccessDeniedHandler` | Token válido, rol insuficiente | **403** `FORBIDDEN` |

Los tres escriben el mismo `ApiError` mediante un **`ApiErrorWriter`** compartido, en vez de repetir la serialización tres veces.

## 7 · `config/CorsConfig.java`

```java
config.setAllowedOrigins(List.of(allowedOrigin));           // app.cors.allowed-origin
config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
config.setAllowedHeaders(List.of("Authorization","Content-Type"));
config.setExposedHeaders(List.of("Content-Disposition"));
```

**`OPTIONS` tiene que estar**: el navegador manda una comprobación previa antes de cualquier `POST` con cabecera `Authorization`, y sin ella frontend ve un error de CORS que no dice nada útil. **`Content-Disposition` expuesto** o las descargas de CSV y PDF llegan sin nombre de archivo.

## 8 · `config/StaticResourceConfig.java` · `/uploads/**`

```java
registry.addResourceHandler("/uploads/**").addResourceLocations("file:./uploads/");
```

**Pide token.** Hay que crear la carpeta con un `.gitkeep` y **añadir `/uploads/` a `.gitignore`**, que hoy no la tiene: si no, las evidencias y portadas de cada una acabarían commiteadas.

> ⚠️ **Consecuencia a revisar con frontend:** las imágenes de portada tampoco cargarán sin token. Como el catálogo también deja de ser público (punto 9), es coherente.

## 9 · El mapa de rutas y roles *(esto es C-06)*

```java
// Públicas · 5
.requestMatchers(POST, "/api/auth/login", "/api/auth/register",
                       "/api/auth/resend-verification").permitAll()
.requestMatchers(GET,  "/api/auth/verify").permitAll()
.requestMatchers(POST, "/api/proposals").permitAll()

// Catálogo · ya no es público
.requestMatchers(GET, "/api/activities", "/api/activities/*")
        .hasAnyRole("EMPLOYEE", "ADMIN")

// Por rol
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/dashboard/**").hasRole("ADMIN")
.requestMatchers("/api/org/**").hasRole("PARTNER")
.requestMatchers("/uploads/**").authenticated()

.anyRequest().authenticated()
```

### Las tres puertas de las propuestas

| Quién | Ruta | Qué hace |
|---|---|---|
| **Sin cuenta** | `POST /api/proposals` | Formulario de la landing · **pública** |
| **PARTNER** | `POST /api/org/proposals` | Proponer desde dentro |
| **PARTNER** | `GET /api/org/proposals` | «Mis propuestas» · solo las suyas |
| **ADMIN** | `GET /api/admin/proposals` | Todas |
| **ADMIN** | `GET`·`accept`·`reject` `/api/admin/proposals/{id}` | Detalle y decisión |

### Ocho rutas que se mueven bajo `/api/admin/`

Compartían prefijo con rutas de otro nivel de acceso, que es el fallo clásico de esta cadena: si se declaran después de las generales, o sin especificar el método, **quedan abiertas**.

| Ruta actual | Ruta nueva |
|---|---|
| `PATCH /api/activities/{id}/cancel` | `PATCH /api/admin/activities/{id}/cancel` |
| `GET`·`PUT /api/activities/{id}/closure` | `GET`·`PUT /api/admin/activities/{id}/closure` |
| `PATCH /api/activities/{id}/closure/finalize` | `PATCH /api/admin/activities/{id}/closure/finalize` |
| `GET /api/proposals` · `GET /api/proposals/{id}` | `GET /api/admin/proposals` · `/{id}` |
| `POST /api/proposals/{id}/accept` | `POST /api/admin/proposals/{id}/accept` |
| `PATCH /api/proposals/{id}/reject` | `PATCH /api/admin/proposals/{id}/reject` |

Con esto la cadena queda **sin una sola colisión de prefijos**: la regla de administración es una línea, y nadie puede abrir una ruta por descuido al añadir un endpoint.

### Tres avisos para el resto del equipo

- **«No administradora» ya no significa «empleada».** Todo `@PreAuthorize` debe nombrar el rol concreto: `hasRole("EMPLOYEE")`, **nunca** `!hasRole("ADMIN")`.
- **`GET /api/activities/{id}` no enseña todo.** La visibilidad por estado —solo `PUBLISHED`, `FULL`, `IN_PROGRESS`, `FINISHED`— es una regla del **servicio**, no de la cadena, y devuelve **404**: quien no debe verla no debe saber que existe.
- **Tres rutas sirven a dos roles**, así que la cadena solo exige token y quién puede lo decide el servicio con `NOT_OWNER`: `PATCH /api/registrations/{id}/cancel`, `GET /api/closures/{id}` y `GET /api/closures/{id}/certificate`.

## 10 · Tests · **aplazados**

`spring-security-test`, la `@TestConfiguration` con la cadena abierta y los `@WithMockUser` de los tres roles quedan **para el final del proyecto**. Es una desviación de los criterios de aceptación de #121 y #157, que hay que anotar.

## 11 · Reponer `application.properties`

El *squash merge* del PR #179 se llevó cinco líneas que C-04 necesita:

```properties
spring.jpa.open-in-view=false
app.base-url=${APP_BASE_URL:http://localhost:5173}
app.cors.allowed-origin=${CORS_ALLOWED_ORIGIN:http://localhost:5173}
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:7200000}
```

> **`app.jwt.secret` sin valor por defecto, a propósito.** Un secreto de desarrollo escrito en el repositorio es exactamente el que acaba en producción. Sin defecto, si falta en `.env` la aplicación no arranca y se nota al momento.

```bash
openssl rand -base64 64      # generar el secreto y ponerlo en .env
```

---

## Archivos

| Archivo | Acción |
|---|---|
| `security/SpringConfig.java` | **Rellenar** |
| `security/JwtService.java` | **Crear** |
| `security/UserDetail.java` | **Crear** |
| `security/CustomAuthenticationManager.java` | **Crear** |
| `security/AccountStatusException.java` | **Crear** |
| `security/PasswordEncoderConfig.java` | **Crear** · separado para evitar el ciclo de *beans* |
| `security/filter/JWTAuthentication.java` | **Crear** · el login |
| `security/filter/JWTAuthorization.java` | **Crear** |
| `security/RestAuthenticationEntryPoint.java` · `RestAccessDeniedHandler.java` · `ApiErrorWriter.java` | **Crear** |
| `config/CorsConfig.java` · `StaticResourceConfig.java` | **Rellenar** |
| `dto/user/UserResponse.java` | **Rellenar** · hoy vacío |
| `dto/auth/LoginRequest.java` · `AuthResponse.java` | **Crear** |
| `repository/UserRepository.java` | **Modificar** · `findByEmail` |
| `controller/ActivityClosureController.java` | **Modificar** · rutas bajo `/api/admin/` |
| `seeder/UserSeeder.java` | **Modificar** · cifrar contraseñas |
| `application.properties` · `.env.example` · `.gitignore` | **Modificar** |
| `docs/api-contract.md` | **Modificar** · rutas nuevas y el catálogo ya no público |

## Verificación

```bash
./mvnw clean verify
```

| Prueba | Esperado |
|---|---|
| `POST /api/auth/login` con credenciales válidas | 200 y `AuthResponse` con el token |
| El mismo, con contraseña incorrecta | **401** genérico, sin decir si el correo existe |
| Login de la cuenta en `PENDING_APPROVAL` | **403** `ACCOUNT_PENDING_APPROVAL` |
| Ruta protegida **sin** token | **401** con cuerpo `ApiError`, no la página de login de Spring |
| Token manipulado o caducado | **401** con `ApiError` |
| `/api/admin/**` con token de empleada | **403**, no 401 |
| `/api/org/**` con token de empleada | **403** |
| `GET /api/activities` con token de entidad | **403** |
| `POST /api/proposals` sin token | pasa · es la única pública de negocio |
| `OPTIONS` desde `localhost:5173` | 200 con cabeceras de CORS |

**401 es «no sé quién eres»; 403 es «sé quién eres y no puedes».** Si una empleada recibe 401 al pedir el dashboard, la cadena está mal montada.

## Enmiendas que genera

| Documento | Qué |
|---|---|
| #121 · #157 | El rol es `PARTNER`, no `ORG` · la librería es `java-jwt`, no `jjwt` · los tests quedan aplazados |
| #120 (B1-02) | **El login ya está hecho en C-04**; esa tarea se queda con `/logout` y `/me` |
| #161 (B1-17) | Los tres códigos de estado de cuenta ya los distingue C-04 |
| `docs/api-contract.md` | El catálogo **ya no es público** · las ocho rutas movidas |
| Runbook | Todo lo anterior |
| #156 (C-05) | Su contenido ya está implementado desde C-02 · probablemente se pueda cerrar |

Se suman a las de C-03, que siguen sin publicar en `docs/c-03-plan.md`.
