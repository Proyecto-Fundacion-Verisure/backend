# Cómo probar los correos del proyecto

Guía corta para las seis. Desde `B3-09` la aplicación manda correos de verdad: trece avisos, con sus plantillas. Este documento explica **dónde aterrizan esos correos mientras desarrollamos** y qué hay que instalar.

**Ningún correo sale nunca a una dirección real.** Las treinta cuentas de empleado de la semilla son inventadas —`ana.gil@verisure.es` y compañía—, así que enviarlas a internet solo generaría rebotes. Todo va a un buzón de pruebas, que es un servidor que recoge los mensajes y los enseña en una bandeja web sin entregárselos a nadie.

> ⚠️ **Esa garantía depende de una sola línea: `MAIL_HOST`.** La aplicación entrega los correos al servidor que diga esa variable y a ningún otro. Mientras valga `localhost` o `sandbox.smtp.mailtrap.io`, los mensajes se quedan en la bandeja de pruebas.
>
> Si alguien pone ahí un servidor de envío real —el SMTP de Gmail, Brevo, SendGrid—, los correos **sí** saldrían a internet. Y ojo: `verisure.es` es un dominio que existe de verdad, así que los mensajes de prueba acabarían en buzones de personas reales.
>
> Por eso `MAIL_HOST` no apunta nunca a un servidor de envío mientras la base de datos tenga datos de prueba, que es siempre en desarrollo.

Usamos dos, y cada uno tiene su momento:

| | Para qué | Límite |
|---|---|---|
| **Mailpit**, en tu máquina | El día a día | Ninguno |
| **Mailtrap**, en la nube | Ensayo y demo | 50 correos al mes en total |

El reparto no es un capricho. La cuenta gratuita de Mailtrap admite **50 correos al mes, uno cada diez segundos y un solo usuario**: entre seis personas probando se agota en una tarde, y un aviso como el de actividad cerrada manda un correo **por participante**, así que una sola prueba con tres inscripciones ya son tres mensajes. Reservamos esa cuota para cuando hay que enseñar una bandeja en la nube.

---

## Mailpit · lo que necesitas para trabajar

Es un buzón falso que corre en tu ordenador. Un único binario, sin cuenta, sin límites y con su propia bandeja web.

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

Es software libre con licencia MIT (`axllent/mailpit`, escrito en Go), mantenido al día y empaquetado en Homebrew, así que no descargas un binario suelto de ninguna web. No toca nada del sistema: abre dos puertos locales y guarda los mensajes en un archivo temporal.

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

No hace falta usuario ni contraseña. Arranca la aplicación y los correos empiezan a aparecer en la bandeja, con su diseño, en cuanto hagas cualquier operación que avise.

---

## Mailtrap · solo para el ensayo y la demo

Cuenta gratuita en <https://mailtrap.io>, sin tarjeta. En **Email Testing → Inboxes → My Inbox → Integrations**, eligiendo **Java / Spring Boot**, salen las credenciales:

```
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USER=...
MAIL_PASSWORD=...
```

Como el plan gratuito es de **un solo usuario**, las credenciales las saca una persona y se pasan por el canal del equipo. Nunca al repositorio: van en `.env`.

Dos cosas que conviene saber para que no parezca que algo está roto. El ritmo permitido es **un correo cada diez segundos**, y nuestros envíos son asíncronos: si salen varios de golpe, los que se pasen del ritmo fallarán. Y los mensajes **se borran a los tres días**.

---

## Qué correos existen y cómo dispararlos

Trece avisos. Seis se pueden provocar hoy desde endpoints ya construidos:

| Operación | Correo que sale |
|---|---|
| `PATCH /api/registrations/{id}/accept` con plazas libres | Plaza confirmada |
| `PATCH /api/registrations/{id}/accept` con el aforo lleno | Estás en lista de espera |
| `PATCH /api/registrations/{id}/reject` | Solicitud no admitida |
| `PATCH /api/registrations/{id}/cancel` de una plaza confirmada | Ha quedado una plaza libre · a quien asciende |
| `POST /api/admin/activities/refresh-status` | Cuéntanos cómo fue · a cada participante |
| `PATCH /api/admin/activities/{id}/closure/finalize` | Certificado disponible · a cada participante |

Los otros siete —actividad cancelada, enviada a revisión, aprobada, devuelta, y los tres del ciclo de la cuenta— saldrán cuando sus endpoints estén.

**El de verificación de correo todavía no está completo**: no existe el token ni su caducidad, que son de `B1-16`. La plantilla está escrita y preparada; hoy el correo sale pero el enlace no verifica nada. En cuanto BE1 entregue el token, se rellena en `MailContentFactory` y queda cerrado.

---

## Si un correo no llega

Mira el registro de la aplicación, porque **el correo nunca tumba la operación de negocio**: si el buzón está caído, la inscripción se confirma igual y el fallo queda como aviso.

```
INFO  ... Correo «Plaza confirmada · Limpieza de playas» enviado a ana.gil@verisure.es
WARN  ... No se pudo enviar «...» a ...: Mail server connection failed
```

- **Ni una línea ni la otra** → no se llegó a pedir el aviso. Suele ser que la operación falló antes, o que se deshizo: una transacción que no confirma no manda correo, y eso es a propósito.
- **`WARN` de conexión** → Mailpit no está arrancado, o el puerto de `.env` no coincide.
- **`INFO` de enviado pero la bandeja vacía** → estás mirando la bandeja equivocada; comprueba si tu `.env` apunta a Mailpit o a Mailtrap.

---

## En resumen

1. `brew install mailpit` una vez.
2. `mailpit` en una terminal mientras trabajas.
3. `MAIL_HOST=localhost` y `MAIL_PORT=1025` en tu `.env`.
4. Bandeja en <http://localhost:8025>.

Y Mailtrap se queda guardado para el día de la presentación.

**`MAIL_HOST` se queda en `localhost`.** Es lo único que separa una bandeja de pruebas de un envío real a direcciones `@verisure.es`.
