from django.urls import path

from . import views, api

urlpatterns = [
    path('api/web/csrf/', api.csrf),
    path('api/web/registro/', api.registro),
    path('api/web/login/', api.login),
    path('api/web/perfil/', api.perfil),
    path('api/web/logout/', api.logout),
    path('', views.inicio, name='inicio'),
    path('login/', views.login_view, name='login'),
    path('registro/', views.registro_view, name='registro'),
    path('salir/', views.logout_view, name='logout'),
]
