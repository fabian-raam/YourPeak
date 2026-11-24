"""Verifica el endpoint web que pide a Spring una propuesta de rutina con IA."""
import io
from unittest.mock import patch
from urllib.error import HTTPError, URLError

from django.test import Client, TestCase, override_settings


@override_settings(SPRING_INTERNAL_API_KEY="test-internal")
class GenerarRutinaApiTests(TestCase):
    ruta = "/api/web/cliente/rutina/generar/"

    def setUp(self):
        session = self.client.session
        session["id_usuario"] = 7
        session.save()
        self.cliente = {"idUsuario": 7, "rol": "CLIENTE", "estado": "ACTIVO"}
        self.propuesta = {"fuente": "GEMINI", "mensaje": "Revísala.", "dias": []}

    @patch("cuentas.api._spring_request")
    def test_cliente_activo_recibe_la_propuesta(self, spring):
        spring.side_effect = [self.cliente, self.propuesta]

        response = self.client.post(self.ruta)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.propuesta)
        self.assertEqual(response["Cache-Control"], "no-store")
        self.assertEqual(spring.call_args.args[0], "/api/cliente/rutina/generar")
        self.assertEqual(spring.call_args.kwargs["method"], "POST")
        self.assertEqual(spring.call_args.kwargs["headers"]["X-Client-User-Id"], "7")
        self.assertGreater(spring.call_args.kwargs["timeout"], 5)

    @patch("cuentas.api._spring_request")
    def test_anonimo_no_puede_generar(self, spring):
        self.assertEqual(Client().post(self.ruta).status_code, 401)
        spring.assert_not_called()

    @patch("cuentas.api._spring_request")
    def test_administrador_no_puede_generar(self, spring):
        spring.return_value = {"idUsuario": 7, "rol": "ADMINISTRADOR"}
        self.assertEqual(self.client.post(self.ruta).status_code, 403)
        self.assertEqual(spring.call_count, 1)

    @patch("cuentas.api._spring_request")
    def test_requiere_csrf(self, spring):
        client = Client(enforce_csrf_checks=True)
        session = client.session
        session["id_usuario"] = 7
        session.save()
        self.assertEqual(client.post(self.ruta).status_code, 403)
        spring.assert_not_called()

    @patch("cuentas.api._spring_request")
    def test_solo_acepta_post(self, spring):
        self.assertEqual(self.client.get(self.ruta).status_code, 405)
        spring.assert_not_called()

    @patch("cuentas.api._spring_request")
    def test_conserva_el_conflicto_de_spring(self, spring):
        conflicto = HTTPError("http://spring/", 409, "Conflict", {}, io.BytesIO(
            b'{"detail":"Marca al menos un d\\u00eda disponible antes de generar tu rutina."}'))
        spring.side_effect = [self.cliente, conflicto]

        response = self.client.post(self.ruta)

        self.assertEqual(response.status_code, 409)
        self.assertIn("día disponible", response.json()["detail"])

    @patch("cuentas.api._spring_request")
    def test_spring_caido_devuelve_503(self, spring):
        spring.side_effect = [self.cliente, URLError("sin conexión")]
        self.assertEqual(self.client.post(self.ruta).status_code, 503)
