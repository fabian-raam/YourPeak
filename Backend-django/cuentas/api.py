"""API web: las cuentas pertenecen a Spring; Django conserva la sesión."""
import json
from urllib.error import HTTPError, URLError

from django.conf import settings
from django.http import JsonResponse
from django.middleware.csrf import get_token
from django.views.decorators.http import require_GET, require_POST

from .validation import validar_correo, validar_fecha
from .views import _spring_request


def error(mensaje, status=400, campos=None):
    return JsonResponse({"detail": mensaje, "campos": campos or {}}, status=status)


def payload(request):
    try:
        data = json.loads(request.body)
    except (ValueError, UnicodeDecodeError):
        return None
    if not isinstance(data, dict) or any(not isinstance(value, str) for value in data.values()):
        return None
    return data


def spring_error(exception):
    try:
        data = json.loads(exception.read().decode("utf-8"))
    except (ValueError, UnicodeDecodeError):
        data = {}
    status = exception.code if exception.code in (400, 401, 409) else 502
    return error(data.get("detail", "No se pudo completar la solicitud."), status, data.get("campos"))


@require_GET
def csrf(request):
    response = JsonResponse({"csrfToken": get_token(request)})
    response["Cache-Control"] = "no-store"
    return response


@require_POST
def registro(request):
    data = payload(request)
    if data is None:
        return error("Envía datos JSON válidos.")
    campos = {}
    for field in ("nombre", "apellido"):
        data[field] = data.get(field, "").strip()
        if not data[field] or len(data[field]) > 100:
            campos[field] = "Ingresa entre 1 y 100 caracteres."
    data["correo"] = data.get("correo", "").strip().lower()
    for field, message in (("correo", validar_correo(data["correo"])),
                           ("fechaNacimiento", validar_fecha(data.get("fechaNacimiento")))):
        if message:
            campos[field] = message
    password = data.get("contrasena", "")
    if not 15 <= len(password) <= 128:
        campos["contrasena"] = "Usa entre 15 y 128 caracteres."
    if password != data.get("confirmar_contrasena"):
        campos["confirmar_contrasena"] = "Las contraseñas no coinciden."
    if campos:
        return error("Revisa los campos indicados.", campos=campos)
    try:
        result = _spring_request("/api/auth/registro", {
            key: data[key] for key in ("nombre", "apellido", "correo", "fechaNacimiento", "contrasena")
        })
    except HTTPError as exception:
        return spring_error(exception)
    except (URLError, TimeoutError, ValueError):
        return error("No se pudo conectar con el servicio. Inténtalo de nuevo.", 503)
    return JsonResponse({"detail": result.get("mensaje", "Tu cuenta fue creada. Ya puedes iniciar sesión.")}, status=201)


@require_POST
def login(request):
    data = payload(request)
    if data is None:
        return error("Envía datos JSON válidos.")
    correo = data.get("correo", "").strip().lower()
    message = validar_correo(correo)
    if message:
        return error(message, campos={"correo": message})
    password = data.get("contrasena", "")
    if not password or len(password) > 128:
        return error("Ingresa tu contraseña.")
    try:
        usuario = _spring_request("/api/auth/login", {"correo": correo, "contrasena": password})
    except HTTPError as exception:
        return spring_error(exception)
    except (URLError, TimeoutError, ValueError):
        return error("No se pudo conectar con el servicio. Inténtalo de nuevo.", 503)
    if not usuario.get("idUsuario"):
        return error("El servicio devolvió una respuesta inválida.", 502)
    request.session.cycle_key()
    request.session["id_usuario"] = usuario["idUsuario"]
    return JsonResponse({"usuario": usuario, "csrfToken": get_token(request)})


@require_GET
def perfil(request):
    user_id = request.session.get("id_usuario")
    if not user_id:
        return error("Inicia sesión para continuar.", 401)
    if not settings.SPRING_INTERNAL_API_KEY:
        return error("El servicio de perfil no está configurado.", 503)
    try:
        usuario = _spring_request(f"/api/auth/usuarios/{user_id}",
                                  headers={"X-Internal-Api-Key": settings.SPRING_INTERNAL_API_KEY})
    except HTTPError as exception:
        if exception.code == 404:
            request.session.flush()
            return error("Tu sesión ya no está disponible.", 401)
        return error("No se pudo cargar tu perfil.", 502)
    except (URLError, TimeoutError, ValueError):
        return error("El servicio de perfil no está disponible.", 503)
    if validar_correo(usuario.get("correo", "").strip().lower()):
        request.session.flush()
        return error("Usa una cuenta institucional para continuar.", 401)
    response = JsonResponse({"usuario": usuario})
    response["Cache-Control"] = "no-store"
    return response


@require_POST
def logout(request):
    request.session.flush()
    return JsonResponse({"detail": "Sesión cerrada."})
