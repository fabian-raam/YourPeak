"""Verifica el límite de permisos entre el navegador y el servicio interno."""
import io
import json
from unittest.mock import patch
from urllib.error import HTTPError

from django.test import Client, TestCase, override_settings


@override_settings(SPRING_INTERNAL_API_KEY="test-internal")
class AdminApiTests(TestCase):
    def setUp(self):
        session = self.client.session
        session["id_usuario"] = 7
        session.save()
        self.admin = {"idUsuario": 7, "rol": "ADMINISTRADOR"}

    @patch("cuentas.api._spring_request")
    def test_anonymous_cannot_list(self, spring):
        response = Client().get("/api/web/admin/clientes/pendientes/")
        self.assertEqual(response.status_code, 401)
        spring.assert_not_called()

    @patch("cuentas.api._spring_request")
    def test_client_cannot_list(self, spring):
        spring.return_value = {"idUsuario": 7, "rol": "CLIENTE", "estado": "ACTIVO"}
        self.assertEqual(self.client.get("/api/web/admin/clientes/pendientes/").status_code, 403)
        self.assertEqual(spring.call_count, 1)

    @patch("cuentas.api._spring_request")
    def test_admin_lists_without_cache(self, spring):
        spring.side_effect = [self.admin, [{"idCliente": 15, "nombre": "Ana"}]]
        response = self.client.get("/api/web/admin/clientes/pendientes/")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()[0]["idCliente"], 15)
        self.assertEqual(response["Cache-Control"], "no-store")
        self.assertEqual(spring.call_args.kwargs["headers"]["X-Admin-User-Id"], "7")

    @patch("cuentas.api._spring_request")
    def test_approval_uses_session_not_supplied_identity(self, spring):
        spring.side_effect = [self.admin, {"estado": "ACTIVO", "detail": "Aprobado"}]
        response = self.client.post("/api/web/admin/clientes/15/aprobar/",
                                    json.dumps({"idAdministrador": "999"}),
                                    content_type="application/json", HTTP_X_ADMIN_USER_ID="999")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(spring.call_args.args[0], "/api/admin/clientes/15/aprobar")
        self.assertEqual(spring.call_args.kwargs["method"], "POST")
        self.assertEqual(spring.call_args.kwargs["headers"]["X-Admin-User-Id"], "7")

    @patch("cuentas.api._spring_request")
    def test_conflict_is_preserved(self, spring):
        spring.side_effect = [self.admin, HTTPError("http://spring/", 409, "Conflict", {},
                                                   io.BytesIO(b'{"detail":"Ya no esta pendiente"}'))]
        self.assertEqual(self.client.post("/api/web/admin/clientes/15/aprobar/").status_code, 409)

    @patch("cuentas.api._spring_request")
    def test_detail_not_found_is_preserved(self, spring):
        spring.side_effect = [self.admin, HTTPError("http://spring/", 404, "Not found", {},
                                                   io.BytesIO(b'{"detail":"No encontrado"}'))]
        self.assertEqual(self.client.get("/api/web/admin/clientes/pendientes/15/").status_code, 404)

    @patch("cuentas.api._spring_request")
    def test_approval_requires_csrf(self, spring):
        client = Client(enforce_csrf_checks=True)
        self.assertEqual(client.post("/api/web/admin/clientes/15/aprobar/").status_code, 403)
        spring.assert_not_called()

    @patch("cuentas.api._spring_request")
    def test_role_is_rechecked_before_approval(self, spring):
        spring.return_value = {"idUsuario": 7, "rol": "CLIENTE"}
        self.assertEqual(self.client.post("/api/web/admin/clientes/15/aprobar/").status_code, 403)
        self.assertEqual(spring.call_count, 1)
