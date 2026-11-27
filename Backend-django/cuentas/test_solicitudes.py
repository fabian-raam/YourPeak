"""Rechazo de solicitudes, nueva solicitud del cliente y actividad del resumen."""
import io
import json
from unittest.mock import patch
from urllib.error import HTTPError

from django.test import Client, TestCase, override_settings


@override_settings(SPRING_INTERNAL_API_KEY="test-internal")
class SolicitudesTests(TestCase):
    def setUp(self):
        session = self.client.session
        session["id_usuario"] = 7
        session.save()

    @patch("cuentas.api._spring_request")
    def test_admin_rechaza_con_motivo(self, spring):
        spring.side_effect = [{"idUsuario": 7, "rol": "ADMINISTRADOR"}, {"estado": "RECHAZADO"}]
        response = self.client.post("/api/web/admin/clientes/15/rechazar/", json.dumps({"motivo": "  Falta el pago  "}),
                                    content_type="application/json")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(spring.call_args.args[0], "/api/admin/clientes/15/rechazar")
        self.assertEqual(spring.call_args.kwargs["payload"], {"motivo": "Falta el pago"})
        self.assertEqual(spring.call_args.kwargs["headers"]["X-Admin-User-Id"], "7")

    @patch("cuentas.api._spring_request")
    def test_rechazo_sin_motivo_no_llega_a_spring(self, spring):
        spring.return_value = {"idUsuario": 7, "rol": "ADMINISTRADOR"}
        response = self.client.post("/api/web/admin/clientes/15/rechazar/", json.dumps({"motivo": " "}),
                                    content_type="application/json")
        self.assertEqual(response.status_code, 400)
        self.assertEqual(spring.call_count, 1)

    @patch("cuentas.api._spring_request")
    def test_cliente_rechazado_consulta_su_solicitud(self, spring):
        spring.side_effect = [{"idUsuario": 7, "rol": "CLIENTE", "estado": "RECHAZADO"},
                              {"estado": "RECHAZADO", "historial": []}]
        response = self.client.get("/api/web/cliente/solicitud/")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(spring.call_args.args[0], "/api/cliente/solicitud")
        self.assertEqual(spring.call_args.kwargs["headers"]["X-Client-User-Id"], "7")

    @patch("cuentas.api._spring_request")
    def test_tope_diario_se_conserva_como_429(self, spring):
        spring.side_effect = [{"idUsuario": 7, "rol": "CLIENTE", "estado": "RECHAZADO"},
                              HTTPError("http://spring/", 429, "Too Many", {},
                                        io.BytesIO('{"detail":"Alcanzaste el máximo"}'.encode()))]
        response = self.client.post("/api/web/cliente/solicitud/", json.dumps({"mensaje": "Ya pagué"}),
                                    content_type="application/json")
        self.assertEqual(response.status_code, 429)
        self.assertEqual(spring.call_args.kwargs["payload"], {"mensaje": "Ya pagué"})

    @patch("cuentas.api._spring_request")
    def test_administrador_no_usa_la_solicitud_de_cliente(self, spring):
        spring.return_value = {"idUsuario": 7, "rol": "ADMINISTRADOR"}
        self.assertEqual(self.client.get("/api/web/cliente/solicitud/").status_code, 403)

    def test_anonimo_no_consulta_solicitud(self):
        self.assertEqual(Client().get("/api/web/cliente/solicitud/").status_code, 401)

    @patch("cuentas.api._spring_request")
    def test_actividad_valida_el_rango(self, spring):
        spring.return_value = {"idUsuario": 7, "rol": "ADMINISTRADOR"}
        self.assertEqual(self.client.get("/api/web/admin/actividad/?dias=500").status_code, 400)
        spring.assert_not_called()

    @patch("administracion.api._spring_request")
    @patch("cuentas.api._spring_request")
    def test_actividad_consulta_spring(self, validar, spring):
        validar.return_value = {"idUsuario": 7, "rol": "ADMINISTRADOR"}
        spring.return_value = [{"fecha": "2026-11-27", "sesiones": 3}]
        response = self.client.get("/api/web/admin/actividad/?dias=14")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(spring.call_args.args[0], "/api/admin/actividad?dias=14")
