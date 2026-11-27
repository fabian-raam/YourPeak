from django.urls import path

from . import views, api, google_auth

urlpatterns = [
    path('api/web/csrf/', api.csrf),
    path('api/web/registro/', api.registro),
    path('api/web/login/', api.login),
    path('api/web/google/config/', google_auth.configuracion),
    path('api/web/google/login/', google_auth.autenticar),
    path('api/web/google/completar/', google_auth.completar),
    path('api/web/perfil/', api.perfil),
    path('api/web/logout/', api.logout),
    # Disponibilidad personal: requiere una sesión de cliente activa.
    path('api/web/cliente/disponibilidad/', api.disponibilidad_cliente),
    path('api/web/cliente/perfil/', api.perfil_cliente, name='perfil-cliente'),
    path('api/web/cliente/membresias/', api.mis_membresias, name='mis-membresias'),
    # Estado de la solicitud de acceso: disponible también para cuentas pendientes o rechazadas.
    path('api/web/cliente/solicitud/', api.solicitud_cliente, name='solicitud-cliente'),
    # Rutina personal: solo permite asignar ejercicios a los días disponibles del cliente.
    path('api/web/cliente/rutina/', api.rutina_cliente, name='rutina-cliente'),
    path('api/web/cliente/rutina/generar/', api.generar_rutina_cliente, name='generar-rutina-cliente'),
    # Asistente con IA: solo alimentación y ejercicio.
    path('api/web/cliente/asistente/', api.asistente_cliente, name='asistente-cliente'),
    path(
        'api/web/cliente/rutina/ejercicios/',
        api.ejercicios_disponibles_rutina,
        name='ejercicios-disponibles-rutina',
    ),
    path(
        'api/web/cliente/rutina/ejercicios/<int:id_plan_ejercicio>/completar/',
        api.completar_ejercicio_rutina,
        name='completar-ejercicio-rutina',
    ),
    # Lista/detalle usan GET; aprobar cambia el estado y por eso usa POST.
    path('api/web/admin/clientes/pendientes/', api.clientes_pendientes),
    path('api/web/admin/clientes/pendientes/<int:id_cliente>/', api.cliente_pendiente),
    path('api/web/admin/clientes/<int:id_cliente>/aprobar/', api.aprobar_cliente),
    path('api/web/admin/clientes/<int:id_cliente>/rechazar/', api.rechazar_cliente),
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
