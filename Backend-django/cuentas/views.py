import json
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from django.conf import settings
from django.shortcuts import redirect, render
from django.views.decorators.http import require_GET, require_http_methods


def _spring_request(path, payload=None, headers=None):
    request_headers = {"Accept": "application/json"}
    request_headers.update(headers or {})
    body = None
    if payload is not None:
        body = json.dumps(payload).encode("utf-8")
        request_headers["Content-Type"] = "application/json"
    request = Request(
        f"{settings.SPRING_API_BASE_URL.rstrip('/')}{path}",
        data=body,
        headers=request_headers,
        method="POST" if body is not None else "GET",
    )
    with urlopen(request, timeout=5) as response:
        return json.loads(response.read().decode("utf-8"))


def _mensaje_error_spring(error):
    try:
        data = json.loads(error.read().decode("utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError):
        return "No se pudo completar la solicitud. Inténtalo de nuevo."
    return data.get("detail") or data.get("mensaje") or "Revisa los datos ingresados."


@require_GET
def inicio(request):
    id_usuario = request.session.get("id_usuario")
    if not id_usuario:
        return redirect("login")
    if not settings.SPRING_INTERNAL_API_KEY:
        return render(request, "cuentas/inicio.html", {
            "error": "Falta configurar SPRING_INTERNAL_API_KEY para consultar tu perfil.",
        }, status=503)

    try:
        usuario = _spring_request(
            f"/api/auth/usuarios/{id_usuario}",
            headers={"X-Internal-Api-Key": settings.SPRING_INTERNAL_API_KEY},
        )
    except HTTPError as error:
        if error.code == 404:
            request.session.flush()
            return redirect("login")
        return render(request, "cuentas/inicio.html", {
            "error": "No se pudo consultar tu perfil en Spring Boot.",
        }, status=502)
    except (URLError, TimeoutError, json.JSONDecodeError, UnicodeDecodeError):
        return render(request, "cuentas/inicio.html", {
            "error": "Spring Boot no está disponible en este momento.",
        }, status=503)
    return render(request, "cuentas/inicio.html", {"usuario": usuario})


@require_http_methods(["GET", "POST"])
def login_view(request):
    if request.session.get("id_usuario"):
        return redirect("inicio")
    error = ""
    if request.method == "POST":
        correo = request.POST.get("correo", "").strip().lower()
        contrasena = request.POST.get("contrasena", "")
        if not correo or not contrasena:
            error = "Ingresa tu correo y contraseña."
        else:
            try:
                usuario = _spring_request("/api/auth/login", {
                    "correo": correo,
                    "contrasena": contrasena,
                })
            except HTTPError as spring_error:
                if spring_error.code == 401:
                    error = "Correo o contraseña incorrectos."
                elif spring_error.code == 403:
                    error = (
                        "Spring Boot rechazó el inicio de sesión (HTTP 403). "
                        "Reinicia Backend-spring para cargar la configuración actual."
                    )
                else:
                    error = _mensaje_error_spring(spring_error)
            except (URLError, TimeoutError, json.JSONDecodeError, UnicodeDecodeError):
                error = "No se pudo conectar con Spring Boot. Inténtalo de nuevo."
            else:
                id_usuario = usuario.get("idUsuario")
                if not id_usuario:
                    error = "Spring Boot devolvió una respuesta inválida."
                else:
                    request.session.cycle_key()
                    request.session["id_usuario"] = id_usuario
                    return redirect("inicio")
    return render(request, "cuentas/login.html", {"error": error})


@require_http_methods(["GET", "POST"])
def registro_view(request):
    error = ""
    datos = {key: request.POST.get(key, "") for key in ("nombre", "apellido", "correo")}
    if request.method == "POST":
        contrasena = request.POST.get("contrasena", "")
        confirmar = request.POST.get("confirmar_contrasena", "")
        if not all(datos.values()) or not contrasena:
            error = "Completa todos los campos."
        elif contrasena != confirmar:
            error = "Las contraseñas no coinciden."
        else:
            try:
                _spring_request("/api/auth/registro", {
                    **datos,
                    "correo": datos["correo"].strip().lower(),
                    "contrasena": contrasena,
                })
            except HTTPError as spring_error:
                error = _mensaje_error_spring(spring_error)
            except (URLError, TimeoutError, json.JSONDecodeError, UnicodeDecodeError):
                error = "No se pudo conectar con Spring Boot. Inténtalo de nuevo."
            else:
                return render(request, "cuentas/registro.html", {
                    "datos": {},
                    "exito": "Tu cuenta fue creada. Ya puedes iniciar sesión.",
                })
    return render(request, "cuentas/registro.html", {"error": error, "datos": datos})


@require_http_methods(["POST"])
def logout_view(request):
    request.session.flush()
    return redirect("login")
