"""Proxy web del catálogo de alimentos administrado por Spring Boot."""
from urllib.error import HTTPError, URLError

from django.views.decorators.http import require_http_methods

from cuentas.api import (
    _headers_administrador,
    _respuesta_spring_json,
    _validar_administrador,
    error,
    objeto_json,
    spring_error,
)
from cuentas.views import _spring_request


def _reenviar_a_spring(request, ruta, metodo, mensaje_error, datos=None, status_exito=200):
    """Valida la sesión administrativa y reenvía la operación al servicio Spring."""
    id_usuario, respuesta_error = _validar_administrador(request)
    if respuesta_error:
        return respuesta_error

    try:
        resultado = _spring_request(
            ruta,
            payload=datos,
            headers=_headers_administrador(id_usuario),
            method=metodo,
        )
    except HTTPError as excepcion:
        return spring_error(excepcion)
    except (URLError, TimeoutError, ValueError):
        return error(mensaje_error, 503)

    return _respuesta_spring_json(resultado, status=status_exito)


@require_http_methods(['GET', 'POST'])
def alimentos(request):
    """Lista los alimentos o registra uno, según el método HTTP."""
    if request.method == 'GET':
        return _reenviar_a_spring(
            request,
            '/api/admin/alimentos',
            'GET',
            'No se pudo consultar el catálogo de alimentos.',
        )

    datos = objeto_json(request)
    if datos is None:
        return error('Envía un objeto JSON válido para registrar el alimento.')
    return _reenviar_a_spring(
        request,
        '/api/admin/alimentos',
        'POST',
        'No se pudo registrar el alimento.',
        datos,
        status_exito=201,
    )


@require_http_methods(['GET', 'PUT'])
def alimento(request, id_alimento):
    """Consulta o actualiza los datos generales y nutricionales del alimento."""
    ruta = f'/api/admin/alimentos/{id_alimento}'
    if request.method == 'GET':
        return _reenviar_a_spring(
            request, ruta, 'GET', 'No se pudo consultar el alimento.'
        )

    datos = objeto_json(request)
    if datos is None:
        return error('Envía un objeto JSON válido para actualizar el alimento.')
    return _reenviar_a_spring(
        request, ruta, 'PUT', 'No se pudo actualizar el alimento.', datos
    )
