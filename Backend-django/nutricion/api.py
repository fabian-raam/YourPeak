"""Proxy de las operaciones de nutrición; Spring conserva y calcula los datos."""
from datetime import date
from urllib.error import HTTPError, URLError

from django.views.decorators.http import require_GET, require_http_methods

from cuentas.api import (
    _headers_cliente,
    _respuesta_spring_json,
    _validar_cliente_activo,
    error,
    objeto_json,
    spring_error,
)
from cuentas.views import _spring_request


def _reenviar_a_spring(request, ruta, metodo, mensaje_error, datos=None, status_exito=200):
    """Valida la sesión activa y reenvía la operación a Spring."""
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
        return error(mensaje_error, 503)

    return _respuesta_spring_json(resultado, status=status_exito)


@require_GET
def alimentos(request):
    """Entrega el catálogo que el cliente puede usar para registrar consumos."""
    return _reenviar_a_spring(
        request,
        '/api/cliente/nutricion/alimentos',
        'GET',
        'No se pudo consultar el catálogo de alimentos.',
    )


@require_GET
def diario(request):
    """Consulta el resumen y los consumos de un día, usando fecha local de Lima por defecto."""
    fecha = request.GET.get('fecha')
    ruta = '/api/cliente/nutricion'
    if fecha:
        try:
            fecha = date.fromisoformat(fecha)
        except ValueError:
            return error('La fecha debe tener formato YYYY-MM-DD.')
        ruta = f'{ruta}?fecha={fecha.isoformat()}'

    return _reenviar_a_spring(
        request,
        ruta,
        'GET',
        'No se pudo consultar el resumen nutricional.',
    )


@require_http_methods(['POST'])
def ingestas(request):
    """Registra un alimento y una cantidad; el cliente sale de la sesión, no del JSON."""
    datos = objeto_json(request)
    if datos is None:
        return error('Envía un objeto JSON con el alimento y la cantidad consumida.')
    return _reenviar_a_spring(
        request,
        '/api/cliente/nutricion/ingestas',
        'POST',
        'No se pudo registrar el consumo.',
        datos,
        status_exito=201,
    )


@require_http_methods(['PUT', 'DELETE'])
def ingesta(request, id_ingesta):
    """Edita o borra un consumo propio; Spring verifica su pertenencia al cliente."""
    datos = None
    if request.method == 'PUT':
        datos = objeto_json(request)
        if datos is None:
            return error('Envía un objeto JSON con el alimento y la cantidad consumida.')

    return _reenviar_a_spring(
        request,
        f'/api/cliente/nutricion/ingestas/{id_ingesta}',
        request.method,
        'No se pudo actualizar o eliminar el consumo.',
        datos,
        status_exito=204 if request.method == 'DELETE' else 200,
    )
