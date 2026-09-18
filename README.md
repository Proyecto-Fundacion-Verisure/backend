# Backend Verisure

Spring Boot REST API for managing Fundación Verisure's volunteer program: activity catalog, registrations, proposals, closures, partner organizations, and impact dashboard.

## Requirements

- Java 25 (JDK).
- PostgreSQL 15 or later, with an empty database for the project.
- Maven is not required: the repository ships the Maven wrapper (`./mvnw`).
- Optional: [Mailpit](https://github.com/axllent/mailpit), a local SMTP test inbox, to see the emails the application sends.
- [Frontend](../frontend) running when testing full flows.

## Dependencies & Tools

| Category | Package | Usage |
| --- | --- | --- |
| Dependencies | ![spring-boot](https://img.shields.io/badge/spring--boot-3.5-6DB33F?logo=springboot&logoColor=white) | Application framework and auto-configuration. |
| | ![spring-web](https://img.shields.io/badge/spring--web-MVC-6DB33F?logo=spring&logoColor=white) | REST controllers and JSON serialization. |
| | ![spring-data-jpa](https://img.shields.io/badge/spring--data--jpa-Hibernate-6DB33F?logo=spring&logoColor=white) | Repositories, projections, and entity mapping. |
| | ![spring-security](https://img.shields.io/badge/spring--security-6-6DB33F?logo=springsecurity&logoColor=white) | Stateless security chain, roles, and method security. |
| | ![java-jwt](https://img.shields.io/badge/java--jwt-4.5-000?logo=auth0&logoColor=white) | JWT signing and verification (Auth0). |
| | ![spring-validation](https://img.shields.io/badge/spring--validation-Bean_Validation-6DB33F?logo=spring&logoColor=white) | Request validation with `@Valid` and a custom date-range constraint. |
| | ![spring-mail](https://img.shields.io/badge/spring--mail-SMTP-6DB33F?logo=spring&logoColor=white) | Asynchronous email notifications. |
| | ![thymeleaf](https://img.shields.io/badge/thymeleaf-3-005F0F?logo=thymeleaf&logoColor=white) | HTML email templates. |
| | ![postgresql](https://img.shields.io/badge/postgresql-driver-4169E1?logo=postgresql&logoColor=white) | Database driver. |
| | ![mapstruct](https://img.shields.io/badge/mapstruct-1.6-E9421E) | Entity ↔ DTO mapping generated at compile time. |
| | ![lombok](https://img.shields.io/badge/lombok-1.18-BC2E2E) | Boilerplate reduction (constructors, getters, setters). |
| | ![openpdf](https://img.shields.io/badge/openpdf-1.3-C00) | PDF export of the impact dashboard. |
| | ![spring-dotenv](https://img.shields.io/badge/spring--dotenv-4.0-ECD53F) | Loads `.env` into the Spring environment. |
| Test | ![junit](https://img.shields.io/badge/junit-5-25A162?logo=junit5&logoColor=white) | Test runner. |
| | ![mockito](https://img.shields.io/badge/mockito-5-78A641) | Mocks for service unit tests and for the services behind the web-layer tests. |
| | ![spring-test](https://img.shields.io/badge/spring--test-MockMvc-6DB33F?logo=spring&logoColor=white) | `@WebMvcTest` integration tests with the real security chain. |
| Tools | ![vscode](https://img.shields.io/badge/VS_Code-007ACC?logo=visualstudio&logoColor=white) | Code editor. |
| | ![postman](https://img.shields.io/badge/Postman-FF6C37?logo=postman&logoColor=white) | Manual endpoint testing. |
| Languages | ![java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white) | Backend language. |

## Installation and first run

```bash
git clone <repository-url>
cd backend
cp .env.example .env
# edit .env: DB_URL, DB_USER, DB_PASSWORD and JWT_SECRET
./mvnw spring-boot:run
```

The API is available at [http://localhost:8080](http://localhost:8080). On the first run Hibernate creates the schema (`ddl-auto=update`) and the seeders load the demo data; both steps are idempotent, so restarting the application never duplicates rows.

Generate the JWT secret with:

```bash
openssl rand -base64 64
```

## Environment variables

All configuration lives in `.env` (ignored by git). `.env.example` lists every variable.

| Variable | Required | Default | Usage |
| --- | --- | --- | --- |
| `DB_URL` | Yes | — | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/db-verisure`. |
| `DB_USER` | Yes | — | Database user. |
| `DB_PASSWORD` | Yes | — | Database password. |
| `JWT_SECRET` | Yes | — | HMAC key that signs the tokens. **The application does not start without it.** |
| `JWT_EXPIRATION_MS` | No | `7200000` | Token lifetime (2 hours). Expiry is the only revocation: logout revokes nothing. |
| `APP_BASE_URL` | No | `http://localhost:5173` | Frontend base URL used to build the links inside emails. |
| `CORS_ALLOWED_ORIGIN` | No | `http://localhost:5173` | Origin allowed by CORS. |
| `MAIL_HOST` | No | *(empty)* | SMTP host: `localhost` with Mailpit running. When empty the application starts anyway and each email is logged as a warning instead of sent. |
| `MAIL_PORT` | No | `1025` | SMTP port (Mailpit's default). |
| `MAIL_USER` | No | *(empty)* | SMTP user. Not needed with Mailpit. |
| `MAIL_PASSWORD` | No | *(empty)* | SMTP password. Not needed with Mailpit. |
| `MAIL_FROM` | No | `no-reply@verisure.ex` | Sender address. |

## Commands

| Command | Description |
| --- | --- |
| `./mvnw spring-boot:run` | Starts the API in development mode with the seeders enabled. |
| `./mvnw test` | Runs all tests: unit tests for services and security (Mockito), and web-layer integration tests (`*IT`) that go through the real JWT filters and `GlobalExceptionHandler` with mocked services. No database is needed. |
| `./mvnw clean package` | Compiles, runs the tests, and builds the jar in `target/`. |
| `java -jar target/backend-0.0.1-SNAPSHOT.jar` | Runs the packaged application. |
| `SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run` | Starts without the seeders (`@Profile("!prod")`). |

## Architecture

```text
src/main/java/com/verisure/backend/
├── config/        # CORS, async mail, scheduling, static /uploads
├── controller/    # REST endpoints, one controller per domain
├── dto/           # Request/response records, grouped by domain
├── entity/        # JPA entities and their enums
├── exception/     # ErrorCode, DomainException, ApiError, global handler
├── mapper/        # MapStruct mappers
├── repository/    # Spring Data repositories and query projections
├── scheduler/     # Nightly activity status transitions
├── security/      # JWT filters, authentication manager, security chain
├── seeder/        # Demo data loaded outside the prod profile
├── service/       # Business rules: interface + Impl per domain
└── validation/    # Custom Bean Validation constraints
src/main/resources/
├── application.properties
└── templates/mail/   # Thymeleaf templates, one per notification
```

Requests flow `controller → service → repository`. Controllers only validate input and delegate; every service is an interface with a single `Impl`; entities never leave the service layer, only DTOs do. Business errors are thrown as `DomainException` with an `ErrorCode` that carries its HTTP status, and `GlobalExceptionHandler` turns every error — business, validation, security, or unexpected — into the same `ApiError` body.

Two background pieces complete the picture: `NotificationService` sends emails asynchronously so a slow SMTP server never delays a response, and `ActivityStatusScheduler` runs every night at 03:00 to move activities to `IN_PROGRESS` and `FINISHED` by date, leaving their confirmed registrations as `PENDING_CLOSURE`.

## API, authentication, and roles

The full contract — routes, DTOs, status codes, and the 18 domain error codes — is in [`docs/api-contract.md`](docs/api-contract.md). It is the source of truth shared with the frontend.

Authentication is a stateless JWT. `POST /api/auth/login` returns the token and the user; every other call sends `Authorization: Bearer <token>`. The token carries the email and the role, so no database lookup happens per request; a role change or an account rejection takes effect when the token expires (2 hours). `401` means "unknown caller", `403` means "known caller, not allowed"; both return `ApiError`.

| Area | Routes | Role |
| --- | --- | --- |
| Public | `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/verify`, `POST /api/auth/resend-verification`, `POST /api/proposals`, `GET /uploads/**` | No token |
| Foundation | `/api/admin/**`, `/api/dashboard/**` | `ADMIN` |
| Catalog | `GET /api/activities`, `GET /api/activities/{id}` | `EMPLOYEE`, `ADMIN` |
| Partner | `/api/org/**` | `PARTNER` |
| Shared | `/api/registrations/**`, `/api/favorites/**`, `/api/closures/**` | Any token; ownership is checked by the service (`NOT_OWNER`) |

Partner accounts go through `PENDING_VERIFICATION` → `PENDING_APPROVAL` → `ACTIVE` (or `REJECTED`). Login checks the password first and the account status second, so the status is only revealed to someone who already knows the password.

## Demo data

Outside the `prod` profile the seeders create partners, users, activities, proposals, registrations, favorites, and closures. Every demo account shares the password `Verisure2026!`.

| Email | Role | Account status |
| --- | --- | --- |
| `carmen.ortega@verisure.ex` | `ADMIN` | Active |
| `ana.gil@verisure.ex` | `EMPLOYEE` | Active |
| `marta.ribas@caritasbcn.ex` | `PARTNER` | Active |
| `pau.estevez@caritasbcn.ex` | `PARTNER` | Pending email verification |
| `elena.vargas@aldeasinfantiles.ex` | `PARTNER` | Pending Foundation approval |
| `rosa.delgado@manosunidas.ex` | `PARTNER` | Rejected |

All seeded addresses use the fictitious `.ex` TLD, so no email can ever reach a real inbox.

## Email

The application sends thirteen notifications (registration confirmed, spot released, activity approved, closure completed, and so on). In development they go to a local Mailpit inbox at [http://localhost:8025](http://localhost:8025), never to the internet: see [`docs/correo-buzon-de-pruebas.md`](docs/correo-buzon-de-pruebas.md) for the setup.

## Authors

| Name | GitHub | Role |
| --- | --- | --- |
| Rosa Vaillant | [@rosana50factoria](https://github.com/rosana50factoria) | Backend |
| Andrea Tapia | [@atapiamallea](https://github.com/atapiamallea) | Backend |
| Chiara Di Maio | [@chdimaio](https://github.com/chdimaio) | Backend |
| Elena Almansa | [@elenaalmansacampos](https://github.com/elenaalmansacampos) | Frontend |
| Fabiana Leonardo | [@fabileoruf](https://github.com/fabileoruf) | Frontend |
| Ivanna Caraccio | [@IvannaRCA](https://github.com/IvannaRCA) | Frontend |

