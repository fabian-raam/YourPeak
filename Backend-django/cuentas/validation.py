from datetime import date
from django.core.exceptions import ValidationError
from django.core.validators import validate_email


def validar_correo(correo):
    try:
        validate_email(correo)
    except ValidationError:
        return "Ingresa un correo válido."
    if correo.rsplit("@", 1)[-1] != "tecsup.edu.pe":
        return "Usa tu correo institucional @tecsup.edu.pe."
    return ""


def validar_fecha(valor):
    try:
        fecha = date.fromisoformat(valor)
    except (ValueError, TypeError):
        return "Ingresa una fecha de nacimiento válida."
    if fecha >= date.today():
        return "La fecha de nacimiento debe ser anterior a hoy."
    return ""
