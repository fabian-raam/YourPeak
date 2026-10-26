from django.urls import path

from . import views, api

urlpatterns = [
    path('api/web/csrf/', api.csrf),
    path('api/web/registro/', api.registro),
    path('api/web/login/', api.login),
    path('api/web/perfil/', api.perfil),
    path('api/web/logout/', api.logout),
    # Lista/detalle usan GET; aprobar cambia el estado y por eso usa POST.
    path('api/web/admin/clientes/pendientes/', api.clientes_pendientes),
    path('api/web/admin/clientes/pendientes/<int:id_cliente>/', api.cliente_pendiente),
    path('api/web/admin/clientes/<int:id_cliente>/aprobar/', api.aprobar_cliente),
    path('', views.inicio, name='inicio'),
    path('login/', views.login_view, name='login'),
    path('registro/', views.registro_view, name='registro'),
    path('salir/', views.logout_view, name='logout'),
]
