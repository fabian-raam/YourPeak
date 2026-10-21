# YourPeak

Sistema para un gimnasio. Spring Boot es dueño de los datos de usuario y se conecta a MySQL. Django sirve temporalmente las páginas de login y registro, guarda la sesión en su propia base SQLite y consulta los datos de usuario mediante endpoints de Spring Boot. React se integrará después.

## Requisitos

- Java 21.
- MySQL o MariaDB en ejecución.
- Python 3.12, 3.13 o 3.14 (versiones compatibles con la versión de Django fijada en `Backend-django/requirements.txt`).
- Git.

## 1. Preparar MySQL para Spring Boot

1. Inicia el servicio local de MySQL/MariaDB.
2. En MySQL Workbench abre y ejecuta, en este orden:
   - `Backend-spring/database/01_estructura.sql`: crea `yourpeak_db` y las tablas incluidas en el esquema del proyecto.
   - `Backend-spring/database/02_catalogos.sql`: carga los estados iniciales del cliente.
3. Los scripts no contienen cuentas reales. Crea una cuenta usando el formulario de registro cuando los servicios estén en marcha.

Estos archivos son el esquema y los catálogos que están guardados en el repositorio; no son una copia completa de los datos de MySQL de otro desarrollador. Spring tiene `ddl-auto=validate`: al arrancar comprueba que las tablas requeridas existan, pero no crea ni modifica el esquema.

## 2. Configurar y arrancar Spring Boot

Abre una terminal en `Backend-spring`. La configuración predeterminada espera MySQL en `localhost:3306`, la base `yourpeak_db`, usuario `root` sin contraseña y Spring Boot en el puerto `8080`. Si tu MySQL tiene otros datos, define las variables antes de arrancar:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/yourpeak_db"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "tu_clave_de_mysql"
```

Si MySQL usa un puerto distinto de `3306`, cambia el puerto en `DB_URL`, por ejemplo `jdbc:mysql://localhost:3307/yourpeak_db`. Si el puerto `8080` está ocupado, cambia el puerto de Spring. Por ejemplo:

```powershell
$env:SERVER_PORT = "8081"
```

Configura también una clave compartida para que Django pueda pedir el perfil del usuario. Usa el mismo valor en las terminales de Spring y Django:

```powershell
$env:SPRING_INTERNAL_API_KEY = "cambia-esto-por-una-clave-local"
```

Arranca Spring:

```powershell
.\mvnw.cmd spring-boot:run
```

Las variables definidas con `$env:` aplican a esa terminal. No guardes contraseñas ni claves reales en los archivos del repositorio.

## 3. Configurar y arrancar Django

Abre otra terminal en `Backend-django`. En la primera ejecución crea el entorno e instala Django:

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Django usa SQLite para su propia sesión; no pongas ahí los usuarios ni configures Django para conectarse a MySQL. Crea las tablas internas de Django y configura la dirección de Spring:

```powershell
.\.venv\Scripts\python.exe manage.py migrate
$env:SPRING_API_BASE_URL = "http://127.0.0.1:8080"
$env:SPRING_INTERNAL_API_KEY = "cambia-esto-por-una-clave-local"
```

Si configuraste Spring en otro puerto, usa ese mismo puerto en `SPRING_API_BASE_URL`; por ejemplo, `http://127.0.0.1:8081`. Después inicia Django:

```powershell
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000
```

Abre `http://127.0.0.1:8000/login/` o `http://127.0.0.1:8000/registro/`. Mantén Spring y Django ejecutándose en sus respectivas terminales.

## Endpoints de autenticación usados por Django

- `POST /api/auth/registro`: crea la cuenta en Spring/MySQL.
- `POST /api/auth/login`: valida el correo y la contraseña en Spring/MySQL.
- `GET /api/auth/usuarios/{idUsuario}`: Django consulta los datos del usuario con el encabezado `X-Internal-Api-Key`.

Si el login acepta las credenciales pero Django muestra que no puede cargar el perfil, comprueba que ambos procesos tengan el mismo `SPRING_INTERNAL_API_KEY` y que `SPRING_API_BASE_URL` apunte al puerto donde está Spring.
