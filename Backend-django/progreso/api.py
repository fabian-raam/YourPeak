"""Proxy del historial físico hacia Spring para el cliente autenticado."""
from urllib.error import HTTPError, URLError

from django.views.decorators.http import require_http_methods

from cuentas.api import (
    _headers_cliente,
    _respuesta_spring_json,
    _validar_cliente_activo,
    error,
    objeto_json,
    spring_error,
)
from cuentas.views import _spring_request


@require_http_methods(['GET', 'POST'])
def progreso_cliente(request):
    """Consulta el historial o registra un peso, usando la identidad de la sesión."""
    datos = None
    metodo = request.method
    ruta = '/api/cliente/progreso'

    if metodo == 'POST':
        datos = objeto_json(request)
        if datos is None:
            return error('Envía un objeto JSON con tu peso en kilogramos.')
        ruta = '/api/cliente/progreso/peso'

    id_usuario, respuesta_error = _validar_cliente_activo(request)
    if respuesta_error:
        return respuesta_error

    try:
        resultado = _spring_request(
            ruta,
            payload=datos,
            headers=_headers_cliente(id_usuario),
            method=metodo,
        )
    except HTTPError as excepcion:
        return spring_error(excepcion)
    except (URLError, TimeoutError, ValueError):
        return error('No se pudo consultar o registrar tu progreso.', 503)

    return _respuesta_spring_json(resultado, status=201 if metodo == 'POST' else 200)
