"""Pruebas de las rutas Django para el catálogo de alimentos."""
import io
import json
from urllib.error import HTTPError, URLError
from unittest.mock import patch

from django.test import Client, TestCase, override_settings


@override_settings(SPRING_INTERNAL_API_KEY='clave-pruebas')
class ApiAlimentosTests(TestCase):
    ruta_base = '/api/web/admin/alimentos/'
    datos_alimento = {
        'nombre': 'Avena',
        'porcionBaseG': 40,
        'kcal': 150,
        'proteinaG': 5,
        'carbohidratoG': 27,
        'grasaG': 3,
    }

    def setUp(self):
        sesion = self.client.session
        sesion['id_usuario'] = 24
        sesion.save()
        self.administrador = {'idUsuario': 24, 'rol': 'ADMINISTRADOR'}

    def enviar_json(self, ruta, metodo='get', datos=None, cliente=None):
        cliente = cliente or self.client
        llamada = getattr(cliente, metodo)
        cuerpo = None if datos is None else json.dumps(datos)
        return llamada(ruta, cuerpo, content_type='application/json')

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_admin_lista_alimentos_sin_cache_y_reenvia_identidad_de_sesion(self, autenticacion, spring):
        alimentos = [{'idAlimento': 2, **self.datos_alimento}]
        autenticacion.return_value = self.administrador
        spring.return_value = alimentos

        respuesta = self.client.get(self.ruta_base)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), alimentos)
        self.assertEqual(respuesta['Cache-Control'], 'no-store')
        self.assertEqual(spring.call_args.args[0], '/api/admin/alimentos')
        self.assertEqual(spring.call_args.kwargs['method'], 'GET')
        self.assertEqual(spring.call_args.kwargs['headers'], {
            'X-Internal-Api-Key': 'clave-pruebas',
            'X-Admin-User-Id': '24',
        })

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_admin_crea_y_conserva_respuesta_201(self, autenticacion, spring):
        autenticacion.return_value = self.administrador
        creado = {'idAlimento': 2, **self.datos_alimento}
        spring.return_value = creado

        respuesta = self.enviar_json(self.ruta_base, 'post', self.datos_alimento)

        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(respuesta.json(), creado)
        self.assertEqual(spring.call_args.args[0], '/api/admin/alimentos')
        self.assertEqual(spring.call_args.kwargs['method'], 'POST')
        self.assertEqual(spring.call_args.kwargs['payload'], self.datos_alimento)

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_admin_consulta_el_detalle_del_alimento(self, autenticacion, spring):
        autenticacion.return_value = self.administrador
        spring.return_value = {'idAlimento': 2, **self.datos_alimento}

        respuesta = self.client.get(self.ruta_base + '2/')

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(spring.call_args.args[0], '/api/admin/alimentos/2')
        self.assertEqual(spring.call_args.kwargs['method'], 'GET')

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_admin_actualiza_valores_nutricionales(self, autenticacion, spring):
        autenticacion.return_value = self.administrador
        spring.return_value = {'idAlimento': 2, **self.datos_alimento}

        respuesta = self.enviar_json(self.ruta_base + '2/', 'put', self.datos_alimento)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(spring.call_args.args[0], '/api/admin/alimentos/2')
        self.assertEqual(spring.call_args.kwargs['method'], 'PUT')
        self.assertEqual(spring.call_args.kwargs['payload'], self.datos_alimento)

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_usuario_anonimo_no_accede_al_catalogo(self, autenticacion, spring):
        respuesta = Client().get(self.ruta_base)

        self.assertEqual(respuesta.status_code, 401)
        autenticacion.assert_not_called()
        spring.assert_not_called()

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_cliente_no_administrador_no_puede_registrar_alimentos(self, autenticacion, spring):
        autenticacion.return_value = {'idUsuario': 24, 'rol': 'CLIENTE', 'estado': 'ACTIVO'}

        respuesta = self.enviar_json(self.ruta_base, 'post', self.datos_alimento)

        self.assertEqual(respuesta.status_code, 403)
        spring.assert_not_called()

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_json_invalido_no_se_reenvia(self, autenticacion, spring):
        respuesta = self.client.post(self.ruta_base, '{json invalido', content_type='application/json')

        self.assertEqual(respuesta.status_code, 400)
        autenticacion.assert_not_called()
        spring.assert_not_called()

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_conserva_conflicto_por_nombre_duplicado(self, autenticacion, spring):
        autenticacion.return_value = self.administrador
        spring.side_effect = HTTPError(
            'http://spring/api/admin/alimentos', 409, 'Conflict', {},
            io.BytesIO(b'{"detail":"Ya existe un alimento con ese nombre."}'),
        )

        respuesta = self.enviar_json(self.ruta_base, 'post', self.datos_alimento)

        self.assertEqual(respuesta.status_code, 409)
        self.assertEqual(respuesta.json()['detail'], 'Ya existe un alimento con ese nombre.')

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_spring_no_disponible_devuelve_503(self, autenticacion, spring):
        autenticacion.return_value = self.administrador
        spring.side_effect = URLError('Connection refused')

        respuesta = self.client.get(self.ruta_base)

        self.assertEqual(respuesta.status_code, 503)

    @patch('alimentos.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_escritura_exige_proteccion_csrf(self, autenticacion, spring):
        cliente_csrf = Client(enforce_csrf_checks=True)

        respuesta = self.enviar_json(
            self.ruta_base, 'post', self.datos_alimento, cliente=cliente_csrf
        )

        self.assertEqual(respuesta.status_code, 403)
        autenticacion.assert_not_called()
        spring.assert_not_called()
