from django.urls import path

from . import api

app_name = 'alimentos'

urlpatterns = [
    path('api/web/admin/alimentos/', api.alimentos, name='lista'),
    path('api/web/admin/alimentos/<int:id_alimento>/', api.alimento, name='detalle'),
]
