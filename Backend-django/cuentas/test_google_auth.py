from datetime import date
from unittest.mock import patch

from django.test import Client, TestCase, override_settings
from google.auth.exceptions import GoogleAuthError, TransportError

from . import google_auth


@override_settings(GOOGLE_OAUTH_CLIENT_ID="client.test", GOOGLE_INTERNAL_KEY="x" * 32)
class GoogleAuthTests(TestCase):
    root = "/api/web/google/"

    def setUp(self):
        self.client = Client(enforce_csrf_checks=True)
        self.csrf = self.client.get("/api/web/csrf/").json()["csrfToken"]
        self.nonce = self.client.get(self.root + "config/").json()["nonce"]
        self.claims = {
            "sub": "1234", "hd": "tecsup.edu.pe", "email": "ANA@tecsup.edu.pe",
            "email_verified": True, "nonce": self.nonce, "given_name": "Ana", "family_name": "Pérez",
        }
        self.verifier = self.enterContext(patch(
            "cuentas.google_auth.id_token.verify_oauth2_token", return_value=self.claims,
        ))
        self.spring = self.enterContext(patch("cuentas.google_auth._spring_request"))
        self.usuario = {"idUsuario": 42, "rol": "CLIENTE", "estado": "PENDIENTE"}

    def post(self, action, data):
        return self.client.post(
            self.root + action + "/", data, content_type="application/json", HTTP_X_CSRFTOKEN=self.csrf,
        )

    def login(self):
        return self.post("login", {"credential": "token-de-prueba"})

    def test_configuracion_sin_cliente_oculta_google(self):
        with self.settings(GOOGLE_OAUTH_CLIENT_ID=""):
            self.assertEqual(self.client.get(self.root + "config/").json(), {"habilitado": False})
            self.assertEqual(self.login().status_code, 503)
            self.spring.assert_not_called()

    def test_rechaza_cuentas_personales_o_sin_dominio_administrado(self):
        for data in (
            {"email": "ana@gmail.com"},
            {"hd": None},
            {"hd": "otro.edu.pe"},
            {"email_verified": False},
        ):
            with self.subTest(data=data):
                with patch.dict(self.claims, data):
                    self.assertEqual(self.login().status_code, 403)
        self.spring.assert_not_called()

    def test_requiere_csrf_y_nonce_de_esta_sesion(self):
        response = self.client.post(self.root + "login/", {"credential": "prueba"}, content_type="application/json")
        self.assertEqual(response.status_code, 403)
        with patch.dict(self.claims, {"nonce": "otro-desafio"}):
            self.assertEqual(self.login().status_code, 401)
        self.spring.assert_not_called()

    def test_fallo_de_google_no_abre_sesion(self):
        for exc, status in ((ValueError(), 401), (GoogleAuthError(), 401), (KeyError(), 401), (TransportError(), 503)):
            with self.subTest(exc=exc):
                self.verifier.side_effect = exc
                self.assertEqual(self.login().status_code, status)
        self.spring.assert_not_called()
        self.assertNotIn("id_usuario", self.client.session)

    def test_primer_registro_pide_fecha_y_usa_solo_identidad_verificada(self):
        self.spring.side_effect = [{"estado": "completar"}, {"estado": "autenticado", "usuario": self.usuario}]
        response = self.login()
        self.assertEqual(response.json(), {
            "estado": "completar", "perfil": {"nombre": "Ana", "apellido": "Pérez", "correo": "ana@tecsup.edu.pe"},
        })
        self.assertEqual(self.verifier.call_args.args[2], "client.test")
        self.assertNotIn("id_usuario", self.client.session)
        self.assertEqual(self.login().status_code, 401)

        data = {"nombre": "Ana", "apellido": "Pérez", "fechaNacimiento": "2000-05-01"}
        response = self.post("completar", {**data, "fechaNacimiento": date.today().isoformat()})
        self.assertEqual(response.status_code, 400)
        self.assertIn("fechaNacimiento", response.json()["campos"])
        response = self.post("completar", {**data, "correo": "otro@tecsup.edu.pe", "subject": "otro"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["usuario"], self.usuario)
        self.spring.assert_called_with(
            "/api/auth/google", {**data, "correo": "ana@tecsup.edu.pe", "subject": "1234"},
            headers={"X-Google-Internal-Key": "x" * 32},
        )
        self.assertEqual(self.client.session["id_usuario"], 42)
        self.assertEqual(self.post("completar", data).status_code, 401)

    def test_usuario_vinculado_recibe_la_sesion_actual_de_yourpeak(self):
        self.spring.return_value = {"estado": "autenticado", "usuario": self.usuario}
        anterior = self.client.session.session_key
        self.assertEqual(self.login().json()["usuario"], self.usuario)
        self.assertEqual(self.client.session["id_usuario"], 42)
        self.assertNotEqual(self.client.session.session_key, anterior)

    def test_cuenta_existente_solicita_contrasena_sin_crear_otra(self):
        self.spring.return_value = {"estado": "vincular"}
        self.assertEqual(self.login().json()["estado"], "vincular")
        self.assertNotIn("id_usuario", self.client.session)
        self.assertEqual(self.post("completar", {"fechaNacimiento": "2000-05-01"}).status_code, 400)
        self.spring.return_value = {"estado": "autenticado", "usuario": self.usuario}
        self.assertEqual(self.post("completar", {"contrasena": "prueba-segura"}).status_code, 200)
        self.assertEqual(self.spring.call_args.args[1]["contrasena"], "prueba-segura")
        self.assertEqual(self.client.session["id_usuario"], 42)

    def test_registro_pendiente_vence(self):
        self.spring.return_value = {"estado": "completar"}
        self.login()
        with patch("cuentas.google_auth.time", return_value=google_auth.time() + 601):
            self.assertEqual(self.post("completar", {}).status_code, 401)
        self.assertEqual(self.spring.call_count, 1)
