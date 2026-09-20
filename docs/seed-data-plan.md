# Datos de prueba · plan aprobado

**2 de septiembre de 2026.** Revisado punto por punto y aprobado.

## Por qué

Los seeders existían vacíos desde C-03. Sin datos, frontend no ve ninguna pantalla con contenido, **el dashboard sale vacío** y nadie puede probar su endpoint a mano.

## Cambio de estructura respecto al runbook

**Son siete seeders, no ocho.** `UserSeeder` y `OrgUserSeeder` se fusionan en un único archivo con los tres roles, porque separarlos no aportaba nada y confundía: `Partner` es la **organización** y `Role.PARTNER` es el **rol de la persona** que trabaja en ella.

Eso obliga a que **`PartnerSeeder` vaya primero**, porque las personas con rol `PARTNER` llevan una FK a su organización.

| `@Order` | Seeder | Filas |
|---:|---|---:|
| 1 | `PartnerSeeder` | 8 |
| 2 | `UserSeeder` · los tres roles | 19 |
| 3 | `ActivitySeeder` | 12 |
| 4 | `ProposalSeeder` | 6 |
| 5 | `RegistrationSeeder` | 10 |
| 6 | `FavoriteSeeder` | 6 |
| 7 | `ClosureSeeder` | 4 + 2 |

`OrgUserSeeder` **se borra**. La fusión resuelve de paso dos problemas que tenía el plan anterior: la guarda de idempotencia rota —dos seeders compartían `UserRepository`, y el segundo no sembraba nunca— y el apaño de dejar `createdBy = null` para rellenarlo después.

**Hay que enmendar** #117, el runbook y **#173**, que se titula «[B3-15] OrgUserSeeder y datos de demostración».

## Restricciones del modelo que condicionan los datos

Verificadas leyendo las ocho entidades:

- `Partner.cif` **único** y de 9 caracteres · `Partner.email` **único y obligatorio**
- `User.email` único · `User.password` **`length = 60`**
- `Registration.accepted` y `Proposal.estimatedVolunteers` son **primitivos**: no admiten `null`
- `ParticipationClosure.registration` y `ActivityClosure.activity` son **únicos**
- `Favorite` tiene único `(activity_id, user_id)`
- `Registration` **no** tiene restricción única `(activity, user)`: es un índice parcial que JPA no expresa, así que los datos no pueden duplicar pares a mano

## Valores que impone frontend

Si no coinciden, los filtros del catálogo no devuelven nada.

- `line`: `desoledad` · `educar` · `acoso` · `medioambiente` — minúsculas, una palabra. La cuarta se llamaba `voluntariado` y se renombró: la etiqueta que enseña frontend es «Medio Ambiente»
- `mode`: `PRESENCIAL` · `ONLINE` · `MIXTO` — mayúsculas

## Fechas · mixtas

- **Pasadas** (`FINISHED`, `CANCELLED`) → fijas de 2026, para que el dashboard por año sea idéntico en las tres máquinas.
- **Vivas** (el resto) → relativas a `LocalDate.now()`, para que siempre haya plazo abierto y la demo de inscribirse funcione dentro de seis meses.

Sin `Random`: dos arranques dan exactamente lo mismo.

## Contraseñas

`password` es obligatorio, así que lleva un marcador con `// TODO C-04`. **El cifrado y el `PasswordEncoder` son de C-04** y no se adelantan aquí.

---

## Los datos

### 1 · `PartnerSeeder` · 8 organizaciones

Seis `ACTIVE`, una `PENDING` (Aldeas Infantiles) y una `REJECTED` (Manos Unidas). Tres nombres —Fundación Solitaria, Educamos Juntos y Prevención Total— coinciden con los mocks del frontend.

### 2 · `UserSeeder` · 19 personas

| Rol | Nº | `organization` | `department` | `partner` |
|---|---:|---|---|---|
| `ADMIN` | 2 | `VERISURE_ES` | Fundación | `null` |
| `EMPLOYEE` | 8 | 5 `VERISURE_ES` · 3 `VERISURE_GROUP` | los 6, rotando | `null` |
| `PARTNER` | 9 | `null` | `null` | FK |

Las 9 de `PARTNER` cubren los cuatro `UserStatus`: 6 `ACTIVE`, 1 `PENDING_VERIFICATION` —una **segunda cuenta de Cáritas**, que reproduce la regla del CIF ya registrado—, 1 `PENDING_APPROVAL` y 1 `REJECTED`.

### 3 · `ActivitySeeder` · 12 actividades

Tres por línea, los **siete** `ActivityStatus` cubiertos. Aforos pequeños (2 a 10) para que con 8 empleadas se puedan llenar de verdad: la actividad `FULL` tiene **2 plazas**.

### 4 · `ProposalSeeder` · 6 propuestas

Dos por estado. La primera lleva **`partner = null`**, porque `POST /api/proposals` es público y una organización sin cuenta puede proponer. Las dos `ACCEPTED` apuntan a actividades distintas.

### 5 · `RegistrationSeeder` · 10 inscripciones

| Actividad | Nº | Estado |
|---|---:|---|
| 1 · Acompañamiento a mayores `FINISHED` | 3 | `CLOSED` ← **las únicas que ve el dashboard** |
| 2 · Alfabetización digital `FINISHED` | 2 | `PENDING_CLOSURE` |
| 9 · Visitas a residencias `FULL` | 3 | 2 `CONFIRMED` (= aforo) · 1 `WAITLISTED` |
| 6 · Limpieza de playas `PUBLISHED` | 1 | `REJECTED` |
| 12 · Autoprotección `CANCELLED` | 1 | `CANCELLED` |

Los seis estados cubiertos. Cinco actividades se quedan sin inscripciones, que es aceptable a esta escala.

### 6 · `FavoriteSeeder` · 6 «me gusta»

Entre 4 empleadas y las actividades publicadas, sin repetir el par único.

### 7 · `ClosureSeeder` · 4 + 2

| Actividad | `ParticipationClosure` | `ActivityClosure` |
|---|---:|---|
| 1 | 3 | `CLOSED`, con `closedAt` |
| 2 | 1 de 2 | `DRAFT` |

Las horas declaradas se separan de las previstas a propósito: es lo que la pantalla de cierre tiene que enseñar. Solo dos de los cuatro llevan evidencia.

---

## Verificación

```sql
select 'partners', count(*) from partners                                        -- 8
union all select 'users', count(*) from users                                    -- 19
union all select 'activities', count(*) from activities                          -- 12
union all select 'proposals', count(*) from proposals                            -- 6
union all select 'registrations', count(*) from registrations                    -- 10
union all select 'favorites', count(*) from favorites                            -- 6
union all select 'participation_closures', count(*) from participation_closures  -- 4
union all select 'activity_closures', count(*) from activity_closures;           -- 2

-- Todos los estados representados
select status, count(*) from registrations group by status;   -- los 6
select status, count(*) from activities    group by status;   -- los 7
select status, count(*) from users         group by status;   -- los 4
select status, count(*) from partners      group by status;   -- los 3
select role,   count(*) from users         group by role;     -- los 3

-- El dashboard NO sale vacío
select count(*) from participation_closures pc
  join registrations r on r.id = pc.registration_id
 where r.status = 'CLOSED';                                   -- 3

-- Idempotencia: tras dos arranques, ningún par repetido
select activity_id, user_id from favorites
 group by activity_id, user_id having count(*) > 1;           -- 0 filas
select activity_id, user_id from registrations
 group by activity_id, user_id having count(*) > 1;           -- 0 filas
```

## Fuera de alcance

- Cifrado de contraseñas y `PasswordEncoder` → **C-04**
- Perfil `test` → **C-04**. Hoy las pruebas corren contra la base de datos de desarrollo
- Las enmiendas a #117, #154, #171, #156, #157, **#173** y el runbook, redactadas y sin publicar
