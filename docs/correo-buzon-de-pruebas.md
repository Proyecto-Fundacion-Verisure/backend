# Cómo probar los correos del proyecto

Guía corta para las seis. La aplicación manda correos de verdad: trece avisos, cada uno con su plantilla. Este documento explica **dónde aterrizan esos correos mientras desarrollamos** y qué hay que instalar.

**Ningún correo sale nunca a una dirección real.** Las cuentas de la semilla son inventadas —`ana.gil@verisure.ex` y compañía—, así que enviarlas a internet solo generaría rebotes. Todo va a un buzón de pruebas, que es un servidor que recoge los mensajes y los enseña en una bandeja web sin entregárselos a nadie.

> ⚠️ **Esa garantía depende de una sola línea: `MAIL_HOST`.** La aplicación entrega los correos al servidor que diga esa variable y a ningún otro. Mientras valga `localhost`, los mensajes se quedan en la bandeja de pruebas.
>
> Si alguien pone ahí un servidor de envío real —el SMTP de Gmail, Brevo, SendGrid—, los correos **sí** saldrían a internet. Las direcciones de la semilla usan el TLD ficticio `.ex` —`@verisure.ex` y compañía—, que no existe de verdad, así que esos mensajes no llegarían a buzones de personas reales.
>
> Por eso `MAIL_HOST` no apunta nunca a un servidor de envío mientras la base de datos tenga datos de prueba, que es siempre en desarrollo.

El buzón de pruebas del proyecto es **Mailpit**, en tu máquina. Sin cuenta, sin límites y con su propia bandeja web.

---

## Mailpit · lo que necesitas para trabajar

Es un buzón falso que corre en tu ordenador: un único binario que abre dos puertos locales y guarda los mensajes en un archivo temporal. No toca nada del sistema.

### Instalación

```bash
# macOS
brew install mailpit

# Linux
sudo sh < <(curl -sL https://raw.githubusercontent.com/axllent/mailpit/develop/install.sh)
```

En **Windows** no hay gestor de paquetes oficial: se descarga el binario de las
[releases del proyecto](https://github.com/axllent/mailpit/releases/latest), se
descomprime y se ejecuta `mailpit.exe`. Es un único archivo, no hay instalador.

Es software libre con licencia MIT (`axllent/mailpit`, escrito en Go), mantenido al día y empaquetado en Homebrew, así que no descargas un binario suelto de ninguna web.

### Arranque

```bash
mailpit
```

Y ya está. Dos puertos:

- **1025** · donde la aplicación deja los correos.
- **8025** · la bandeja, en <http://localhost:8025>.

Déjalo abierto en una pestaña de la terminal mientras trabajas.

### Configuración

En tu `.env` —que está ignorado por git, así que esto es tuyo y de nadie más—:

```
MAIL_HOST=localhost
MAIL_PORT=1025
```

No hace falta usuario ni contraseña: `MAIL_USER` y `MAIL_PASSWORD` se dejan vacíos. `1025` es ya el valor por defecto de `MAIL_PORT`, así que basta con poner el host. Arranca la aplicación y los correos empiezan a aparecer en la bandeja, con su diseño, en cuanto hagas cualquier operación que avise.

---

## Qué correos existen y cómo dispararlos

Trece avisos, todos con su endpoint. La contraseña de todas las cuentas de demostración es `Verisure2026!`.

| Operación | Correo que sale | A quién |
|---|---|---|
| `PATCH /api/registrations/{id}/accept` con plazas libres | Plaza confirmada | La persona inscrita |
| `PATCH /api/registrations/{id}/accept` con el aforo lleno | Estás en lista de espera | La persona inscrita |
| `PATCH /api/registrations/{id}/reject` | Solicitud no admitida | La persona inscrita |
| `PATCH /api/registrations/{id}/cancel` de una plaza confirmada | Ha quedado una plaza libre | Quien asciende de la cola |
| `PATCH /api/admin/activities/{id}/cancel` | Actividad cancelada | Cada persona con inscripción viva |
| `POST /api/admin/activities/refresh-status` (o la tarea de las 03:00) cuando una actividad pasa a `FINISHED` | Cuéntanos cómo fue | Cada participante |
| `PATCH /api/admin/activities/{id}/closure/finalize` | Certificado disponible | Cada participante |
| `PATCH /api/org/activities/{id}/submit` | Actividad enviada a revisión | La Fundación |
| `PATCH /api/admin/activities/{id}/approve` | Actividad aprobada | La entidad |
| `PATCH /api/admin/activities/{id}/return` | Actividad devuelta · lleva el `reviewNote` | La entidad |
| `PATCH /api/admin/org-accounts/{id}/approve` | Cuenta aprobada | La cuenta de entidad |
| `PATCH /api/admin/org-accounts/{id}/reject` | Cuenta rechazada | La cuenta de entidad |
| `POST /api/auth/register` y `POST /api/auth/resend-verification` | Verifica tu correo · enlace con token de 24 horas | La cuenta recién creada |

Los envíos son asíncronos y **el correo nunca tumba la operación de negocio**: si el buzón está caído, la inscripción se confirma igual y el fallo queda como aviso en el registro.

---

## Si un correo no llega

Mira el registro de la aplicación:

```
INFO  ... Correo «Plaza confirmada · Limpieza de playas» enviado a ana.gil@verisure.ex
WARN  ... No se pudo enviar «...» a ...: Mail server connection failed
```

- **Ni una línea ni la otra** → no se llegó a pedir el aviso. Suele ser que la operación falló antes, o que se deshizo: una transacción que no confirma no manda correo, y eso es a propósito.
- **`WARN` de conexión** → Mailpit no está arrancado, o el puerto de `.env` no coincide con el 1025.
- **`WARN` sin `MAIL_HOST`** → la variable está vacía: la aplicación arranca igual, pero cada correo queda solo como aviso.

---

## En resumen

1. `brew install mailpit` una vez.
2. `mailpit` en una terminal mientras trabajas.
3. `MAIL_HOST=localhost` en tu `.env`.
4. Bandeja en <http://localhost:8025>.

**`MAIL_HOST` se queda en `localhost`.** Es lo único que separa una bandeja de pruebas de un envío real a direcciones `@verisure.ex`.
