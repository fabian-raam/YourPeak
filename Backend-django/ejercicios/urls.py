from django.urls import path, re_path

from . import api

app_name = 'ejercicios'

urlpatterns = [
    re_path(
        r'^api/web/ejercicios/imagenes/(?P<nombre>[a-f0-9]{32}\.(?:jpg|png))/$',
        api.imagen_ejercicio,
        name='imagen',
    ),
    path('api/web/admin/ejercicios/', api.ejercicios, name='lista'),
    path('api/web/admin/ejercicios/catalogos/', api.catalogos, name='catalogos'),
    path('api/web/admin/ejercicios/<int:id_ejercicio>/', api.ejercicio, name='detalle'),
    path(
        'api/web/admin/ejercicios/<int:id_ejercicio>/estado/',
        api.estado_ejercicio,
        name='estado',
    ),
]
