from django.urls import path

from . import api

app_name = 'nutricion'

urlpatterns = [
    path('api/web/cliente/nutricion/', api.diario, name='diario'),
    path('api/web/cliente/nutricion/alimentos/', api.alimentos, name='alimentos'),
    path('api/web/cliente/nutricion/ingestas/', api.ingestas, name='ingestas'),
    path('api/web/cliente/nutricion/ingestas/<int:id_ingesta>/', api.ingesta, name='ingesta'),
]
