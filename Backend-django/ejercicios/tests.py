"""Pruebas de la API Django que actúa como proxy del catálogo de Spring."""
import io
import json
from urllib.error import HTTPError, URLError
from unittest.mock import patch

from django.test import Client, TestCase, override_settings


@override_settings(SPRING_INTERNAL_API_KEY='clave-pruebas')
class ApiEjerciciosTests(TestCase):
    ruta_base = '/api/web/admin/ejercicios/'

    def setUp(self):
        sesion = self.client.session
        sesion['id_usuario'] = 24
        sesion.save()
        self.administrador = {'idUsuario': 24, 'rol': 'ADMINISTRADOR'}

    def json(self, ruta, metodo='get', datos=None, cliente=None):
        cliente = cliente or self.client
        llamada = getattr(cliente, metodo)
        cuerpo = None if datos is None else json.dumps(datos)
        return llamada(ruta, cuerpo, content_type='application/json')

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_administrador_lista_ejercicios_sin_cache(self, auth, spring):
        ejercicios = [{'idEjercicio': 5, 'nombre': 'Sentadilla', 'estado': 'ACTIVO'}]
        auth.return_value = self.administrador
        spring.return_value = ejercicios

        respuesta = self.client.get(self.ruta_base)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), ejercicios)
        self.assertEqual(respuesta['Cache-Control'], 'no-store')
        self.assertEqual(spring.call_args.args[0], '/api/admin/ejercicios')
        self.assertEqual(spring.call_args.kwargs['method'], 'GET')
        self.assertEqual(spring.call_args.kwargs['headers'], {
            'X-Internal-Api-Key': 'clave-pruebas',
            'X-Admin-User-Id': '24',
        })

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_administrador_consulta_catalogos(self, auth, spring):
        catalogos = {'gruposMusculares': [], 'nivelesExperiencia': [], 'tiposMedicion': []}
        auth.return_value = self.administrador
        spring.return_value = catalogos

        respuesta = self.client.get(self.ruta_base + 'catalogos/')

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), catalogos)
        self.assertEqual(spring.call_args.args[0], '/api/admin/ejercicios/catalogos')

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_administrador_consulta_detalle(self, auth, spring):
        detalle = {'idEjercicio': 8, 'nombre': 'Press de banca'}
        auth.return_value = self.administrador
        spring.return_value = detalle

        respuesta = self.client.get(self.ruta_base + '8/')

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), detalle)
        self.assertEqual(spring.call_args.args[0], '/api/admin/ejercicios/8')

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_administrador_crea_ejercicio_y_conserva_201(self, auth, spring):
        solicitud = {
            'idGrupoMuscular': 2,
            'idNivelExperiencia': 1,
            'idTipoMedicion': 1,
            'nombre': 'Sentadilla',
            'descripcion': 'Flexión de rodillas',
            'equipamiento': 'Barra',
            'imagenRuta': 'sentadilla.png',
        }
        creado = {'idEjercicio': 5, **solicitud, 'estado': 'ACTIVO'}
        auth.return_value = self.administrador
        spring.return_value = creado

        respuesta = self.json(self.ruta_base, 'post', solicitud)

        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(respuesta.json(), creado)
        self.assertEqual(spring.call_args.args[0], '/api/admin/ejercicios')
        self.assertEqual(spring.call_args.kwargs['method'], 'POST')
        self.assertEqual(spring.call_args.kwargs['payload'], solicitud)

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_administrador_actualiza_ejercicio(self, auth, spring):
        solicitud = {
            'idGrupoMuscular': 2,
            'idNivelExperiencia': 1,
            'idTipoMedicion': 1,
            'nombre': 'Sentadilla frontal',
            'descripcion': None,
            'equipamiento': 'Barra',
            'imagenRuta': None,
        }
        auth.return_value = self.administrador
        spring.return_value = {'idEjercicio': 5, **solicitud, 'estado': 'ACTIVO'}

        respuesta = self.json(self.ruta_base + '5/', 'put', solicitud)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(spring.call_args.args[0], '/api/admin/ejercicios/5')
        self.assertEqual(spring.call_args.kwargs['method'], 'PUT')
        self.assertEqual(spring.call_args.kwargs['payload'], solicitud)

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_administrador_cambia_estado_sin_eliminar_ejercicio(self, auth, spring):
        auth.return_value = self.administrador
        spring.return_value = {'idEjercicio': 5, 'estado': 'INACTIVO'}

        respuesta = self.json(self.ruta_base + '5/estado/', 'patch', {'estado': 'INACTIVO'})

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(spring.call_args.args[0], '/api/admin/ejercicios/5/estado')
        self.assertEqual(spring.call_args.kwargs['method'], 'PATCH')
        self.assertEqual(spring.call_args.kwargs['payload'], {'estado': 'INACTIVO'})

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_usuario_anonimo_no_accede_ni_llama_a_spring(self, auth, spring):
        cliente_anonimo = Client()

        respuesta = cliente_anonimo.get(self.ruta_base)

        self.assertEqual(respuesta.status_code, 401)
        auth.assert_not_called()
        spring.assert_not_called()

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_cliente_autenticado_no_accede_al_catalogo(self, auth, spring):
        auth.return_value = {'idUsuario': 24, 'rol': 'CLIENTE', 'estado': 'ACTIVO'}

        respuesta = self.client.get(self.ruta_base)

        self.assertEqual(respuesta.status_code, 403)
        auth.assert_called_once()
        spring.assert_not_called()

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_rechaza_json_malformado_sin_llamar_a_spring(self, auth, spring):
        respuesta = self.client.post(self.ruta_base, '{json incorrecto', content_type='application/json')

        self.assertEqual(respuesta.status_code, 400)
        auth.assert_not_called()
        spring.assert_not_called()

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_conserva_conflicto_devuelto_por_spring(self, auth, spring):
        auth.return_value = self.administrador
        spring.side_effect = HTTPError(
            'http://spring/api/admin/ejercicios', 409, 'Conflict', {},
            io.BytesIO(b'{"detail":"Ya existe un ejercicio con ese nombre."}'),
        )

        respuesta = self.json(self.ruta_base, 'post', {'nombre': 'Sentadilla'})

        self.assertEqual(respuesta.status_code, 409)
        self.assertEqual(respuesta.json()['detail'], 'Ya existe un ejercicio con ese nombre.')

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_spring_no_disponible_responde_503(self, auth, spring):
        auth.return_value = self.administrador
        spring.side_effect = URLError('Connection refused')

        respuesta = self.client.get(self.ruta_base)

        self.assertEqual(respuesta.status_code, 503)

    @patch('ejercicios.api._spring_request')
    @patch('cuentas.api._spring_request')
    def test_escritura_exige_token_csrf(self, auth, spring):
        cliente_con_csrf = Client(enforce_csrf_checks=True)
        respuesta = self.json(
            self.ruta_base,
            'post',
            {'nombre': 'Sentadilla'},
            cliente=cliente_con_csrf,
        )

        self.assertEqual(respuesta.status_code, 403)
        auth.assert_not_called()
        spring.assert_not_called()
