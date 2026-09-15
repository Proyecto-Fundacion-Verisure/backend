# Plan para el frontend: `medio_ambiente` → `medioambiente` y adiós a la portada

> Para el equipo de frontend · repo `Proyecto-Fundacion-Verisure/frontend`, rama `dev`.
> Va junto al PR de backend `b2-03` (elimina `POST /api/admin/activity-images` y el campo `imageUrl`).

## Contexto

- El **backend guarda `medioambiente`** (una palabra) en `line`, y ya es lo que siembran
  `ActivitySeeder` y `ProposalSeeder`. El **frontend sigue usando `medio_ambiente`** (dos
  palabras con guion). Resultado: el filtro «Medio ambiente» del catálogo no devuelve nada
  contra el backend real, y su imagen por defecto rota (404).
- El **backend ya no tiene portada**: ni campo `imageUrl` en `Activity` ni en los DTO, ni
  endpoint `POST /api/admin/activity-images` (el PR lo borra; esa llamada ahora responde 404).
  Las imágenes pasan a ser una **imagen por defecto por línea** que resuelve el propio frontend,
  igual que ya hace `ProposalForm` con `getLineByValue(values.line)?.image`.
- `ProposalForm.jsx` **no hay que tocarlo**: ya usa `ACTIVITY_LINES` para imagen y etiqueta.

## Tarea 1 · Renombrar `medio_ambiente` → `medioambiente`

Buscar todas las apariciones de `medio_ambiente` en el repo y cambiarlas a `medioambiente`
(minúsculas, una palabra). Conocidas:

| Archivo | Qué cambia |
|---|---|
| `src/constants/activityLines.js` | `value: "medio_ambiente"` → `"medioambiente"` (y la ruta de imagen, ver Tarea 2) |
| `src/features/activities/ActivityCard.jsx` | clave `medio_ambiente` en `LINE_LABELS` |
| `src/features/activities/ActivityDetailPage.jsx` | clave `medio_ambiente` en `LINE_LABELS` |
| `src/features/activities/CatalogPage.jsx` | `value: 'medio_ambiente'` en `LINE_OPTIONS` |
| `src/features/activities/ActivityFormPage.jsx` | `<option value="medio_ambiente">Medio ambiente</option>` |
| `src/api/activitiesApi.js` | mock id 4: `line: 'medio_ambiente'` |
| `src/api/proposalsApi.js` | mock id 4: `line: 'medio_ambiente'` |
| `public/demo-data.json` | actividades id 4: `"line": "medio_ambiente"` |
| `scripts/restore-demo.js` | si duplica el valor, el mismo cambio |
| `src/test/fixtures/proposals.js` | el valor si aparece en el fixture |

> Cualquier otra aparición que dé el `grep` global, la misma regla. El **valor** es
> `medioambiente`; **no** se tocan las etiquetas visibles («Medio ambiente»).

## Tarea 2 · Arreglar la imagen de la línea «Medio ambiente»

`ACTIVITY_LINES` apunta a `/images/04-medio_ambiente-linea-de-accion.png`, pero el archivo que
existe es `public/images/04-voluntariado-linea-de-accion.png` (las 01/02/03 sí cuadran).
Hoy, usado, da 404.

**Opción A (recomendada):** renombrar el archivo a `public/images/04-medioambiente-linea-de-accion.png`
y apuntar `ACTIVITY_LINES[3].image` a esa misma ruta. Coherente con el patrón `0X-<línea>-linea-de-accion.png`.
**Opción B:** no renombrar el archivo y apuntar la constante a la ruta existente
`/images/04-voluntariado-linea-de-accion.png`. Menos correos, pero el nombre no casa con la línea.

> Acordar la ruta final con el PR de backend si el tamaño importa: 01/02/03 ya se sirven igual.

## Tarea 3 · Quitar el flujo de subida de portada y derivar imagen desde la línea

- **`src/api/activitiesApi.js`**: borrar `uploadActivityImage` (llamaba a
  `POST /admin/activity-images`, que ya no existe). `mockGetAdminActivity` puede seguir
  devolviendo `imageUrl: activity.image` en modo mock; en el payload real no se envía.
- **`src/features/activities/ActivityFormPage.jsx`**: quitar el import de
  `uploadActivityImage`, `imageUrl` de `initialValues`, las constantes `MAX_IMAGE_SIZE` y
  `ALLOWED_IMAGE_TYPES`, `validateImage`, el estado `imageFile`, el campo `imageUrl` de
  `buildPayload`, `buildRequestPayload` y el `<input type="file">` (incluido el input de URL
  para role `PARTNER`). La imagen se enseña por defecto según la línea; no hace falta control
  de imagen en el formulario.
- **`src/features/activities/ActivityCard.jsx`** y **`ActivityDetailPage.jsx`**: hoy muestran
  `activity.image`, que el backend ya no devuelve (quedaría «Sin imagen»). Derivarla por línea:
  `getLineByValue(activity.line)?.image ?? <placeholder>`.
- Mientras el backend ignore campos desconocidos (así es por defecto), mandar `imageUrl` de más
  no rompe **nada**; pero hay que dejar de mandarlo y, sobre todo, dejar de llamar al endpoint.

## Coordinación

- **Del propio PR**: el flujo de subida y el campo `imageUrl` murieron en el mismo PR de backend;
  el PR de frontend debe retirarlos en el mismo bordado, si no la creación de actividades
  reventaría con 404 en cuanto se integre.
- **BE1** (`ParticipationClosureServiceImpl`): su comentario interno aún cita `POST /api/admin/activity-images`
  como patrón de la evidenvia; queda sin efecto. Aviso pendiente, no bloquea nada.
- `docs/seed-data-plan.md` ya fija `medioambiente`; no cambia nada.

## Verificación

1. `grep -r "medio_ambiente"` en el repo del frontend → **0 resultados**.
2. Filtro «Medio ambiente» del catálogo → devuelve las actividades sembradas por el backend.
3. Crear y editar una actividad de línea «Medio ambiente» → funciona y la tarjeta enseña su
   imagen de línea (sin URL rota).
4. `uploadActivityImage` y `POST /admin/activity-images` → no quedan referencias.