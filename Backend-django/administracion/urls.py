from django.urls import path

from . import api

app_name = 'administracion'

urlpatterns = [
    path('api/web/admin/resumen/', api.resumen, name='resumen'),
    path('api/web/admin/entrenamientos/', api.entrenamientos, name='entrenamientos'),
    path(
        'api/web/admin/entrenamientos/<int:id_cliente>/',
        api.entrenamiento_cliente,
        name='entrenamiento-cliente',
    ),
]
