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


@override_settings(SPRING_INTERNAL_API_KEY="test-internal")
class PlanMembresiaApiTests(TestCase):
    def setUp(self):
        session = self.client.session
        session["id_usuario"] = 7
        session.save()
        self.admin = {"idUsuario": 7, "rol": "ADMINISTRADOR"}

    def post_json(self, path, data):
        return self.client.post(
            path,
            json.dumps(data),
            content_type="application/json",
        )

    @patch("cuentas.api._spring_request")
    def test_anonymous_cannot_create_plan(self, spring):
        response = Client().post(
            "/api/web/admin/planes-membresia/",
            json.dumps({"nombre": "Plan mensual", "duracionMeses": 1}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 401)
        spring.assert_not_called()

    @patch("cuentas.api._spring_request")
    def test_non_admin_cannot_change_plan(self, spring):
        spring.return_value = {"idUsuario": 7, "rol": "CLIENTE", "estado": "ACTIVO"}
        response = self.post_json(
            "/api/web/admin/planes-membresia/",
            {"nombre": "Plan mensual", "duracionMeses": 1},
        )
        self.assertEqual(response.status_code, 403)
        self.assertEqual(spring.call_count, 1)

    @patch("cuentas.api._spring_request")
    def test_admin_creates_plan_and_preserves_created_status(self, spring):
        created = {
            "idPlanMembresia": 3,
            "nombre": "Plan trimestral",
            "duracionMeses": 3,
            "estado": "ACTIVO",
        }
        spring.side_effect = [self.admin, created]

        response = self.post_json(
            "/api/web/admin/planes-membresia/",
            {"nombre": "Plan trimestral", "duracionMeses": 3},
        )

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json(), created)
        self.assertEqual(spring.call_args.args[0], "/api/admin/planes-membresia")
        self.assertEqual(spring.call_args.kwargs["method"], "POST")
        self.assertEqual(spring.call_args.kwargs["payload"]["duracionMeses"], 3)
        self.assertEqual(spring.call_args.kwargs["headers"]["X-Admin-User-Id"], "7")

    @patch("cuentas.api._spring_request")
    def test_admin_updates_only_plan_name(self, spring):
        updated = {
            "idPlanMembresia": 3,
            "nombre": "Trimestral renovado",
            "duracionMeses": 3,
            "estado": "ACTIVO",
        }
        spring.side_effect = [self.admin, updated]

        response = self.client.put(
            "/api/web/admin/planes-membresia/3/",
            json.dumps({"nombre": "Trimestral renovado"}),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(spring.call_args.kwargs["method"], "PUT")
        self.assertEqual(spring.call_args.kwargs["payload"], {"nombre": "Trimestral renovado"})

    @patch("cuentas.api._spring_request")
    def test_admin_changes_plan_state_with_patch(self, spring):
        updated = {
            "idPlanMembresia": 3,
            "nombre": "Plan trimestral",
            "duracionMeses": 3,
            "estado": "INACTIVO",
        }
        spring.side_effect = [self.admin, updated]

        response = self.client.patch(
            "/api/web/admin/planes-membresia/3/estado/",
            json.dumps({"estado": "INACTIVO"}),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(spring.call_args.kwargs["method"], "PATCH")
        self.assertEqual(spring.call_args.kwargs["payload"], {"estado": "INACTIVO"})

    @patch("cuentas.api._spring_request")
    def test_create_plan_requires_csrf(self, spring):
        client = Client(enforce_csrf_checks=True)
        response = client.post(
            "/api/web/admin/planes-membresia/",
            json.dumps({"nombre": "Plan mensual", "duracionMeses": 1}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)
        spring.assert_not_called()
