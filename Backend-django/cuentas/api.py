"""API web: las cuentas pertenecen a Spring; Django conserva la sesión."""
import json
from urllib.error import HTTPError, URLError

from django.conf import settings
from django.http import JsonResponse
from django.middleware.csrf import get_token
from django.views.decorators.http import require_GET, require_http_methods, require_POST

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


def objeto_json(request):
    """Lee un objeto JSON para las APIs que admiten valores numéricos además de texto."""
    try:
        data = json.loads(request.body)
    except (ValueError, UnicodeDecodeError):
        return None
    return data if isinstance(data, dict) else None


def spring_error(exception):
    """Conserva errores esperados de Spring y transforma fallos internos en 502."""
    try:
        data = json.loads(exception.read().decode("utf-8"))
    except (ValueError, UnicodeDecodeError):
        data = {}
    status = exception.code if exception.code in (400, 401, 403, 404, 409) else 502
    return error(data.get("detail", "No se pudo completar la solicitud."), status, data.get("campos"))


def _validar_administrador(request):
    # React no envia una identidad confiable: el usuario se obtiene de la sesion Django.
    """Obtiene el ID de sesión y verifica el rol consultando a Spring."""
    id_usuario = request.session.get("id_usuario")
    if not id_usuario:
        return None, error("Inicia sesión como administrador para continuar.", 401)

    if not settings.SPRING_INTERNAL_API_KEY:
        return None, error("El servicio interno no está configurado.", 503)

    try:
        # Spring es la fuente de verdad del rol; se consulta antes de cada operacion admin.
        usuario = _spring_request(
            f"/api/auth/usuarios/{id_usuario}",
            headers={"X-Internal-Api-Key": settings.SPRING_INTERNAL_API_KEY},
        )
    except HTTPError as exception:
        if exception.code == 404:
            request.session.flush()
            return None, error("Tu sesión ya no está disponible.", 401)
        return None, error("No se pudo validar el acceso administrativo.", 502)
    except (URLError, TimeoutError, ValueError):
        return None, error("El servicio de usuarios no está disponible.", 503)

    if usuario.get("rol") != "ADMINISTRADOR":
        return None, error("Solo un administrador puede realizar esta operación.", 403)

    return id_usuario, None


def _headers_administrador(id_usuario):
    """Prepara la clave interna y el ID validado que Spring comprobara nuevamente."""
    return {
        "X-Internal-Api-Key": settings.SPRING_INTERNAL_API_KEY,
        "X-Admin-User-Id": str(id_usuario),
    }


def _respuesta_spring_json(data, status=200):
    """Devuelve JSON sin permitir que el navegador almacene la respuesta administrativa."""
    response = JsonResponse(data, safe=isinstance(data, dict), status=status)
    response["Cache-Control"] = "no-store"
    return response


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
    if not 8 <= len(password) <= 128:
        campos["contrasena"] = "Usa entre 8 y 128 caracteres."
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


@require_GET
def clientes_pendientes(request):
    """Lista clientes pendientes solo para una sesion cuyo rol sea administrador."""
    id_usuario, respuesta_error = _validar_administrador(request)
    if respuesta_error:
        return respuesta_error

    try:
        # Django hace de intermediario; la clave interna nunca se envia desde React.
        clientes = _spring_request(
            "/api/admin/clientes/pendientes",
            headers=_headers_administrador(id_usuario),
        )
    except HTTPError as exception:
        return spring_error(exception)
    except (URLError, TimeoutError, ValueError):
        return error("No se pudo consultar la lista de clientes pendientes.", 503)

    return _respuesta_spring_json(clientes)


@require_GET
def cliente_pendiente(request, id_cliente):
    """Obtiene desde Spring el detalle de un cliente que sigue pendiente."""
    id_usuario, respuesta_error = _validar_administrador(request)
    if respuesta_error:
        return respuesta_error

    try:
        cliente = _spring_request(
            f"/api/admin/clientes/pendientes/{id_cliente}",
            headers=_headers_administrador(id_usuario),
        )
    except HTTPError as exception:
        return spring_error(exception)
    except (URLError, TimeoutError, ValueError):
        return error("No se pudo consultar el cliente pendiente.", 503)

    return _respuesta_spring_json(cliente)


@require_POST
def aprobar_cliente(request, id_cliente):
    """Pide a Spring que apruebe el cliente usando la sesion admin ya validada."""
    id_usuario, respuesta_error = _validar_administrador(request)
    if respuesta_error:
        return respuesta_error

    try:
        # Este POST no requiere cuerpo JSON, por eso se indica el metodo explicitamente.
        resultado = _spring_request(
            f"/api/admin/clientes/{id_cliente}/aprobar",
            headers=_headers_administrador(id_usuario),
            method="POST",
        )
    except HTTPError as exception:
        return spring_error(exception)
    except (URLError, TimeoutError, ValueError):
        return error("No se pudo aprobar el cliente.", 503)

    return _respuesta_spring_json(resultado)


def _solicitar_plan_membresia(request, ruta, metodo, mensaje_error, data=None):
    """Valida al administrador y reenvía la gestión del plan a Spring."""
    id_usuario, respuesta_error = _validar_administrador(request)
    if respuesta_error:
        return respuesta_error

    try:
        resultado = _spring_request(
            ruta,
            payload=data,
            headers=_headers_administrador(id_usuario),
            method=metodo,
        )
    except HTTPError as exception:
        return spring_error(exception)
    except (URLError, TimeoutError, ValueError):
        return error(mensaje_error, 503)

    # Conserva 201 Created en el alta en lugar de convertir la respuesta a 200.
    return _respuesta_spring_json(resultado, status=201 if metodo == "POST" else 200)


@require_http_methods(["GET", "POST"])
def planes_membresia(request):
    """Lista planes o crea uno nuevo, según el método HTTP usado."""
    if request.method == "POST":
        data = objeto_json(request)
        if data is None:
            return error("Envía un objeto JSON válido.")
        return _solicitar_plan_membresia(
            request,
            "/api/admin/planes-membresia",
            "POST",
            "No se pudo crear el plan de membresía.",
            data,
        )

    return _solicitar_plan_membresia(
        request,
        "/api/admin/planes-membresia",
        "GET",
        "No se pudo consultar los planes de membresía.",
    )


@require_http_methods(["GET", "PUT"])
def plan_membresia(request, id_plan):
    """Consulta o actualiza nombre y duración de un plan por su identificador."""
    if request.method == "PUT":
        data = objeto_json(request)
        if data is None:
            return error("Envía un objeto JSON válido.")
        return _solicitar_plan_membresia(
            request,
            f"/api/admin/planes-membresia/{id_plan}",
            "PUT",
            "No se pudo actualizar el plan de membresía.",
            data,
        )

    return _solicitar_plan_membresia(
        request,
        f"/api/admin/planes-membresia/{id_plan}",
        "GET",
        "No se pudo consultar el plan de membresía.",
    )


@require_http_methods(["PATCH"])
def cambiar_estado_plan_membresia(request, id_plan):
    """Activa o desactiva lógicamente un plan sin borrar su historial."""
    data = objeto_json(request)
    if data is None:
        return error("Envía un objeto JSON válido.")
    return _solicitar_plan_membresia(
        request,
        f"/api/admin/planes-membresia/{id_plan}/estado",
        "PATCH",
        "No se pudo cambiar el estado del plan de membresía.",
        data,
    )


@require_POST
def logout(request):
    request.session.flush()
    return JsonResponse({"detail": "Sesión cerrada."})
