from django.urls import path

from . import api

app_name = 'progreso'

urlpatterns = [
    path('api/web/cliente/progreso/', api.progreso_cliente, name='progreso-cliente'),
]
