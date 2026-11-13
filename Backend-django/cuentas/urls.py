from django.urls import path

from . import views, api

urlpatterns = [
    path('api/web/csrf/', api.csrf),
    path('api/web/registro/', api.registro),
    path('api/web/login/', api.login),
    path('api/web/perfil/', api.perfil),
    path('api/web/logout/', api.logout),
    # Disponibilidad personal: requiere una sesión de cliente activa.
    path('api/web/cliente/disponibilidad/', api.disponibilidad_cliente),
    # Rutina personal: solo permite asignar ejercicios a los días disponibles del cliente.
    path('api/web/cliente/rutina/', api.rutina_cliente, name='rutina-cliente'),
    path(
        'api/web/cliente/rutina/ejercicios/',
        api.ejercicios_disponibles_rutina,
        name='ejercicios-disponibles-rutina',
    ),
    # Lista/detalle usan GET; aprobar cambia el estado y por eso usa POST.
    path('api/web/admin/clientes/pendientes/', api.clientes_pendientes),
    path('api/web/admin/clientes/pendientes/<int:id_cliente>/', api.cliente_pendiente),
    path('api/web/admin/clientes/<int:id_cliente>/aprobar/', api.aprobar_cliente),
    path('api/web/admin/clientes/<int:id_cliente>/membresias/', api.membresias_cliente),
    path('api/web/admin/clientes/activos/', api.clientes_activos),
    path('api/web/admin/clientes/inactivos/', api.clientes_inactivos),
    # Gestión administrativa del catálogo de planes de membresía.
    path('api/web/admin/planes-membresia/', api.planes_membresia),
    path('api/web/admin/planes-membresia/<int:id_plan>/', api.plan_membresia),
    path('api/web/admin/planes-membresia/<int:id_plan>/estado/', api.cambiar_estado_plan_membresia),
    path('', views.inicio, name='inicio'),
    path('login/', views.login_view, name='login'),
    path('registro/', views.registro_view, name='registro'),
    path('salir/', views.logout_view, name='logout'),
]
