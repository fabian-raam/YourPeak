from django.urls import path

from . import api

app_name = 'ejercicios'

urlpatterns = [
    path('api/web/admin/ejercicios/', api.ejercicios, name='lista'),
    path('api/web/admin/ejercicios/catalogos/', api.catalogos, name='catalogos'),
    path('api/web/admin/ejercicios/<int:id_ejercicio>/', api.ejercicio, name='detalle'),
    path(
        'api/web/admin/ejercicios/<int:id_ejercicio>/estado/',
        api.estado_ejercicio,
        name='estado',
    ),
]
