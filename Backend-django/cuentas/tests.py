import json
from unittest.mock import patch
from django.test import Client, SimpleTestCase, TestCase, override_settings
from .validation import validar_correo, validar_fecha


class ValidationTests(SimpleTestCase):
    def test_exact_domain(self):
        self.assertEqual(validar_correo("ana@tecsup.edu.pe"), "")
        for value in ("ana@gmail.com", "ana@sub.tecsup.edu.pe", "ana@tecsup.edu.pe.evil.com", "ana@@tecsup.edu.pe", "@tecsup.edu.pe"):
            self.assertTrue(validar_correo(value), value)

    def test_birth_date(self):
        self.assertEqual(validar_fecha("2000-02-29"), "")
        for value in (None, "", "2023-02-29", "2999-01-01"):
            self.assertTrue(validar_fecha(value))


@override_settings(SPRING_INTERNAL_API_KEY="test-key")
class AuthApiTests(TestCase):
    def post(self, path, data):
        return self.client.post("/api/web/" + path + "/", json.dumps(data), content_type="application/json")

    @patch("cuentas.api._spring_request")
    def test_registration_passes_birth_date(self, spring):
        spring.return_value = {"mensaje": "Cuenta creada"}
        response = self.post("registro", {"nombre": " Ana ", "apellido": "Perez", "fechaNacimiento": "2000-02-29", "correo": " ANA@TECSUP.EDU.PE ", "contrasena": "una frase de prueba larga", "confirmar_contrasena": "una frase de prueba larga"})
        self.assertEqual(response.status_code, 201)
        sent = spring.call_args.args[1]
        self.assertEqual(sent["fechaNacimiento"], "2000-02-29")
        self.assertEqual(sent["correo"], "ana@tecsup.edu.pe")
        self.assertNotIn("confirmar_contrasena", sent)

    @patch("cuentas.api._spring_request")
    def test_external_login_rejected_before_spring(self, spring):
        response = self.post("login", {"correo": "ana@gmail.com", "contrasena": "secret"})
        self.assertEqual(response.status_code, 400)
        spring.assert_not_called()

    @patch("cuentas.api._spring_request")
    def test_missing_birth_date_rejected(self, spring):
        response = self.post("registro", {"nombre": "Ana", "apellido": "Perez", "correo": "ana@tecsup.edu.pe", "contrasena": "una frase de prueba larga", "confirmar_contrasena": "una frase de prueba larga"})
        self.assertEqual(response.status_code, 400)
        self.assertIn("fechaNacimiento", response.json()["campos"])
        spring.assert_not_called()

    @patch("cuentas.api._spring_request")
    def test_session_profile_logout(self, spring):
        user = {"idUsuario": 17, "nombre": "Ana", "correo": "ana@tecsup.edu.pe"}
        spring.return_value = user
        self.assertEqual(self.client.get("/api/web/perfil/").status_code, 401)
        self.assertEqual(self.post("login", {"correo": user["correo"], "contrasena": "secret"}).status_code, 200)
        self.assertEqual(dict(self.client.session), {"id_usuario": 17})
        self.assertEqual(self.client.get("/api/web/perfil/").json()["usuario"], user)
        self.assertEqual(spring.call_args.kwargs["headers"], {"X-Internal-Api-Key": "test-key"})
        self.assertEqual(self.post("logout", {}).status_code, 200)
        self.assertEqual(self.client.get("/api/web/perfil/").status_code, 401)

    def test_csrf_required_on_all_mutations(self):
        client = Client(enforce_csrf_checks=True)
        for path in ("registro", "login", "logout"):
            self.assertEqual(client.post(f"/api/web/{path}/", "{}", content_type="application/json").status_code, 403)
        token = client.get("/api/web/csrf/").json()["csrfToken"]
        response = client.post("/api/web/logout/", "{}", content_type="application/json", HTTP_X_CSRFTOKEN=token)
        self.assertEqual(response.status_code, 200)

    def test_invalid_json(self):
        response = self.client.post("/api/web/login/", "[]", content_type="application/json")
        self.assertEqual(response.status_code, 400)
