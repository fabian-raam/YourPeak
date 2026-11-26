"""Acceso web con Google Workspace de Tecsup."""
import re
from secrets import compare_digest, token_urlsafe
from time import time
from urllib.error import HTTPError, URLError

from django.conf import settings
from django.views.decorators.cache import never_cache
from django.views.decorators.http import require_GET, require_POST
from google.auth.exceptions import GoogleAuthError, TransportError
from google.auth.transport.requests import Request
from google.oauth2 import id_token
from requests import Session

from .api import _respuesta_spring_json, error, iniciar_sesion, payload, spring_error
from .validation import validar_correo, validar_fecha
from .views import _spring_request


DOMINIO = "tecsup.edu.pe"
DURACION = 600
CLAVE_NONCE = "google_nonce"
CLAVE_PENDIENTE = "google_pendiente"


def _habilitado():
    return bool(settings.GOOGLE_OAUTH_CLIENT_ID and len(settings.GOOGLE_INTERNAL_KEY) >= 32)


class GoogleRequest(Request):
    def __call__(self, url, method="GET", body=None, headers=None, timeout=5, **kwargs):
        return super().__call__(url, method=method, body=body, headers=headers, timeout=timeout, **kwargs)


@require_GET
@never_cache
def configuracion(request):
    if not _habilitado():
        return _respuesta_spring_json({"habilitado": False})
    challenge = request.session.get(CLAVE_NONCE)
    if not challenge or challenge["vence"] <= time():
        challenge = {"valor": token_urlsafe(32), "vence": time() + DURACION}
        request.session[CLAVE_NONCE] = challenge
    return _respuesta_spring_json({
        "habilitado": True,
        "clientId": settings.GOOGLE_OAUTH_CLIENT_ID,
        "nonce": challenge["valor"],
        "dominio": DOMINIO,
    })


@require_POST
@never_cache
def autenticar(request):
    if not _habilitado():
        return error("El acceso con Google no está disponible.", 503)
    data = payload(request)
    credential = data.get("credential", "") if data is not None else ""
    if not credential or len(credential) > 8192:
        return error("Selecciona tu cuenta de Google para continuar.")
    challenge = request.session.get(CLAVE_NONCE)
    if not challenge or challenge["vence"] <= time():
        return error("La selección de Google venció. Vuelve a elegir tu cuenta.", 401)
    try:
        with Session() as session:
            claims = id_token.verify_oauth2_token(
                credential, GoogleRequest(session=session), settings.GOOGLE_OAUTH_CLIENT_ID,
            )
    except TransportError:
        return error("No se pudo verificar tu cuenta con Google. Inténtalo de nuevo.", 503)
    except (GoogleAuthError, ValueError, KeyError):
        return error("Google no pudo verificar tu cuenta. Vuelve a intentarlo.", 401)

    nonce = claims.get("nonce")
    if not isinstance(nonce, str) or not compare_digest(challenge["valor"].encode(), nonce.encode()):
        return error("La selección de Google venció. Vuelve a elegir tu cuenta.", 401)

    correo = str(claims.get("email", "")).strip().lower()
    if (claims.get("hd") != DOMINIO or claims.get("email_verified") is not True
            or len(correo) > 254 or validar_correo(correo)):
        return error("Usa tu cuenta institucional @tecsup.edu.pe para continuar.", 403)

    subject = claims.get("sub")
    if not isinstance(subject, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,255}", subject):
        return error("Google no pudo verificar tu cuenta. Vuelve a intentarlo.", 401)

    identidad = {
        "subject": subject,
        "correo": correo,
        "nombre": str(claims.get("given_name") or "").strip()[:100],
        "apellido": str(claims.get("family_name") or "").strip()[:100],
    }
    return _resolver_cuenta(request, identidad)


@require_POST
@never_cache
def completar(request):
    if not _habilitado():
        return error("El acceso con Google no está disponible.", 503)

    pendiente = request.session.get(CLAVE_PENDIENTE)
    if not pendiente or pendiente["vence"] <= time():
        request.session.pop(CLAVE_PENDIENTE, None)
        return error("El registro con Google venció. Vuelve a elegir tu cuenta.", 401)
    data = payload(request)
    if data is None:
        return error("Envía datos JSON válidos.")

    identidad = pendiente["identidad"].copy()
    if pendiente["estado"] == "vincular":
        password = data.get("contrasena", "")
        if not password or len(password) > 128:
            return error("Ingresa tu contraseña de YourPeak.", campos={"contrasena": "Ingresa tu contraseña."})
        identidad["contrasena"] = password
    else:
        campos = {}
        for field in ("nombre", "apellido"):
            identidad[field] = data.get(field, "").strip()
            if not identidad[field] or len(identidad[field]) > 100:
                campos[field] = "Ingresa entre 1 y 100 caracteres."
        fecha = data.get("fechaNacimiento", "")
        mensaje = validar_fecha(fecha)
        if mensaje:
            campos["fechaNacimiento"] = mensaje
        if campos:
            return error("Revisa los campos indicados.", campos=campos)
        identidad["fechaNacimiento"] = fecha

    return _resolver_cuenta(request, identidad, pendiente)


def _resolver_cuenta(request, identidad, pendiente=None):
    try:
        resultado = _spring_request(
            "/api/auth/google", identidad,
            headers={"X-Google-Internal-Key": settings.GOOGLE_INTERNAL_KEY},
        )
    except HTTPError as exception:
        if exception.code == 403:
            return error("El servicio de acceso con Google no está configurado correctamente.", 503)
        return spring_error(exception)
    except (URLError, TimeoutError, ValueError):
        return error("El servicio de cuentas no está disponible. Inténtalo de nuevo.", 503)

    estado = resultado.get("estado") if isinstance(resultado, dict) else None
    if estado == "autenticado" and isinstance(resultado.get("usuario"), dict):
        return iniciar_sesion(request, resultado["usuario"])
    if estado not in ("completar", "vincular"):
        return error("El servicio devolvió una respuesta inválida.", 502)

    perfil = pendiente["identidad"] if pendiente else identidad
    request.session.pop(CLAVE_NONCE, None)
    request.session[CLAVE_PENDIENTE] = {
        "estado": estado, "identidad": perfil, "vence": pendiente["vence"] if pendiente else time() + DURACION,
    }
    return _respuesta_spring_json({
        "estado": estado,
        "perfil": {key: perfil[key] for key in ("nombre", "apellido", "correo")},
    })
