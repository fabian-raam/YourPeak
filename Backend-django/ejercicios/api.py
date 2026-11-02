"""Proxy web del catálogo de ejercicios administrado por Spring Boot."""
from urllib.error import HTTPError, URLError

from django.views.decorators.http import require_GET, require_http_methods

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
    """Valida la sesión administrativa y reenvía la operación al backend dueño de los datos."""
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
def ejercicios(request):
    """Lista los ejercicios o crea uno, según el método HTTP."""
    if request.method == 'GET':
        return _reenviar_a_spring(
            request,
            '/api/admin/ejercicios',
            'GET',
            'No se pudo consultar el catálogo de ejercicios.',
        )

    datos = objeto_json(request)
    if datos is None:
        return error('Envía un objeto JSON válido para crear el ejercicio.')
    return _reenviar_a_spring(
        request,
        '/api/admin/ejercicios',
        'POST',
        'No se pudo crear el ejercicio.',
        datos,
        status_exito=201,
    )


@require_GET
def catalogos(request):
    """Devuelve las opciones de grupo, experiencia y medición desde Spring."""
    return _reenviar_a_spring(
        request,
        '/api/admin/ejercicios/catalogos',
        'GET',
        'No se pudieron consultar los catálogos de ejercicios.',
    )


@require_http_methods(['GET', 'PUT'])
def ejercicio(request, id_ejercicio):
    """Consulta o reemplaza los datos editables de un ejercicio."""
    ruta = f'/api/admin/ejercicios/{id_ejercicio}'
    if request.method == 'GET':
        return _reenviar_a_spring(
            request, ruta, 'GET', 'No se pudo consultar el ejercicio.'
        )

    datos = objeto_json(request)
    if datos is None:
        return error('Envía un objeto JSON válido para actualizar el ejercicio.')
    return _reenviar_a_spring(
        request,
        ruta,
        'PUT',
        'No se pudo actualizar el ejercicio.',
        datos,
    )


@require_http_methods(['PATCH'])
def estado_ejercicio(request, id_ejercicio):
    """Activa o desactiva lógicamente un ejercicio sin eliminarlo."""
    datos = objeto_json(request)
    if datos is None:
        return error('Envía un objeto JSON válido con el estado del ejercicio.')
    return _reenviar_a_spring(
        request,
        f'/api/admin/ejercicios/{id_ejercicio}/estado',
        'PATCH',
        'No se pudo cambiar el estado del ejercicio.',
        datos,
    )
