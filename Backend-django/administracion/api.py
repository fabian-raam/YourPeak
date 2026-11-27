"""Endpoints web del panel administrativo que consultan datos en Spring."""
from urllib.error import HTTPError, URLError

from django.views.decorators.http import require_GET

from cuentas.api import (
    _headers_administrador,
    _respuesta_spring_json,
    _validar_administrador,
    error,
    spring_error,
)
from cuentas.views import _spring_request


def _consultar_spring(request, ruta, mensaje_error):
    """Valida la sesión administrativa y reenvía la consulta a Spring sin exponer la API key."""
    id_usuario, respuesta_error = _validar_administrador(request)
    if respuesta_error:
        return respuesta_error

    try:
        resultado = _spring_request(
            ruta,
            headers=_headers_administrador(id_usuario),
            method='GET',
        )
    except HTTPError as excepcion:
        return spring_error(excepcion)
    except (URLError, TimeoutError, ValueError):
        return error(mensaje_error, 503)

    return _respuesta_spring_json(resultado)


@require_GET
def resumen(request):
    """Entrega a React las métricas del inicio después de validar que sea administrador."""
    return _consultar_spring(
        request,
        '/api/admin/resumen',
        'No se pudo consultar el resumen administrativo.',
    )


@require_GET
def entrenamientos(request):
    """Lista rutinas actuales y actividad reciente de clientes activos o inactivos."""
    return _consultar_spring(
        request,
        '/api/admin/entrenamientos',
        'No se pudo consultar las rutinas y la actividad.',
    )


@require_GET
def entrenamiento_cliente(request, id_cliente):
    """Devuelve la rutina semanal y los últimos entrenamientos de un cliente."""
    return _consultar_spring(
        request,
        f'/api/admin/entrenamientos/{id_cliente}',
        'No se pudo consultar el entrenamiento del cliente.',
    )


@require_GET
def actividad(request):
    """Sesiones de entrenamiento por día para el gráfico del resumen (7 a 60 días)."""
    dias = request.GET.get('dias', '14')
    if not dias.isdigit() or not 7 <= int(dias) <= 60:
        return error('Indica entre 7 y 60 días.')
    return _consultar_spring(
        request,
        f'/api/admin/actividad?dias={int(dias)}',
        'No se pudo consultar la actividad de entrenamiento.',
    )
