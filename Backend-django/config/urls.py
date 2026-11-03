from django.urls import include, path

urlpatterns = [
    path('', include('cuentas.urls')),
    path('', include('ejercicios.urls')),
    path('', include('alimentos.urls')),
]
