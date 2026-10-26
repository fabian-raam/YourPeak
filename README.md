# YourPeak

## Contexto del proyecto

YourPeak es una plataforma de gestión y seguimiento de entrenamiento para un gimnasio. La idea general contempla cuentas de clientes, membresías, registro físico, disponibilidad, rutinas, ejercicios y sesiones de entrenamiento. El esquema SQL que está ahora en el repositorio cubre solo una parte de esa base; no representa todavía todas las áreas del diseño.

La aplicación web utiliza React como interfaz y Django como backend web, además de la aplicación móvil Android. Spring Boot ofrece los servicios de cuenta y conserva los datos de usuario en MySQL. La base de Django es SQLite y se usa para la sesión web; no es una copia de MySQL ni debe almacenar las cuentas de usuario.

## Arquitectura y estado actual

| Parte | Tecnología | Estado |
|---|---|---|
| Servicio de cuentas | Spring Boot, Java 21 y MySQL | Tiene registro y login por endpoints. Usa `yourpeak_db`; la conexión se configura en `Backend-spring/src/main/resources/application.properties`. |
| Web/backend | Django y SQLite | Expone la API web para React, llama a los endpoints de Spring Boot y conserva solo el ID del usuario en su sesión SQLite. Mantiene los formularios provisionales. |
| Web/interfaz final | React | Registro, login, perfil básico y cierre de sesión implementados con React y Vite. Diseño adaptable y animaciones suaves basados en Figma. |
| Aplicación móvil | Android, Kotlin y Jetpack Compose | Registro, login, perfil básico y cierre de sesión conectados directamente a Spring Boot con Retrofit. Diseño en tonos pastel. |
| Base de datos | MySQL/MariaDB | El esquema completo y sus catálogos iniciales están en `Backend-spring/database/yourpeak_schema.sql`. No contiene cuentas reales. |

### Recorrido actual de login

1. La persona completa el formulario de React. React envía la solicitud a Django mediante `/api/web/login/`, con la protección CSRF correspondiente.
2. Django reenvía correo y contraseña a Spring Boot mediante `POST /api/auth/login`; Django no comprueba contraseñas ni busca cuentas en SQLite.
3. Spring Boot valida la contraseña y consulta la cuenta en MySQL. El registro funciona igual: Django envía los datos a `POST /api/auth/registro` y Spring crea el usuario y su perfil de cliente en MySQL.
4. Si el login es correcto, Django guarda solo el ID de usuario en su sesión SQLite. Al cerrar sesión, Django borra esa sesión.
5. Para mostrar el perfil, Django pide `GET /api/auth/usuarios/{idUsuario}` a Spring usando el encabezado `X-Internal-Api-Key`.

Por tanto, MySQL es la fuente de verdad para las cuentas. SQLite guarda los datos de sesión propios de Django y no es una copia de MySQL. Django y Spring comparten una clave interna de desarrollo definida en sus archivos de configuración para que Django pueda consultar el perfil y comunicarse con los endpoints internos.

## Requisitos

- Java 21.
- MySQL o MariaDB en ejecución.
- Python 3.12, 3.13 o 3.14 (versiones compatibles con la versión de Django fijada en `Backend-django/requirements.txt`).
- Git.
- Node.js y npm para ejecutar el frontend React. Se verificó localmente con Node.js 24 y npm 11.

## 1. Preparar MySQL para Spring Boot

1. Inicia el servicio local de MySQL/MariaDB.
2. En MySQL Workbench abre y ejecuta `Backend-spring/database/yourpeak_schema.sql`. El archivo crea `yourpeak_db`, las tablas y los catálogos iniciales.
3. Ejecútalo sobre una base vacía. Si ya existe una base de prueba con tablas, expórtala primero si quieres conservar sus datos y luego vacíala o elimínala antes de cargar el esquema nuevo: el archivo no borra ni reemplaza tablas existentes.
4. El SQL no contiene cuentas reales. Crea una cuenta desde el formulario de registro cuando Spring Boot, Django y React estén en marcha.

Este archivo es el esquema y los catálogos iniciales del proyecto; no es una copia de los datos de MySQL de otro desarrollador. Spring tiene `ddl-auto=validate`: al arrancar comprueba que las tablas que usa existan, pero no crea ni modifica el esquema.

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

Arranca Spring:

```powershell
.\mvnw.cmd spring-boot:run
```

La clave interna de desarrollo ya está configurada en el repositorio y coincide con el valor de `Backend-django/config/settings.py`. En este proyecto académico se usa un valor compartido para facilitar las pruebas locales; no debe reutilizarse fuera del entorno de desarrollo.

## 3. Configurar y arrancar Django

Abre otra terminal en `Backend-django`. En la primera ejecución crea el entorno e instala Django:

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Si tienes Python 3.13 o 3.14, sustituye `-3.12` por la versión instalada al crear el entorno.

Django usa SQLite para su propia sesión; no pongas ahí los usuarios ni configures Django para conectarse a MySQL. Crea las tablas internas de Django y configura la dirección de Spring:

```powershell
.\.venv\Scripts\python.exe manage.py migrate
$env:SPRING_API_BASE_URL = "http://127.0.0.1:8080"
```

Si configuraste Spring en otro puerto, usa ese mismo puerto en `SPRING_API_BASE_URL`; por ejemplo, `http://127.0.0.1:8081`. Después inicia Django:

```powershell
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000
```

Mantén Spring y Django ejecutándose en sus respectivas terminales y continúa con el paso 4 para abrir la interfaz React. Las direcciones `http://127.0.0.1:8000/login/` y `http://127.0.0.1:8000/registro/` conservan las páginas provisionales de Django.

## Endpoints de autenticación usados por Django

- `POST /api/auth/registro`: crea la cuenta en Spring/MySQL.
- `POST /api/auth/login`: valida el correo y la contraseña en Spring/MySQL.
- `GET /api/auth/usuarios/{idUsuario}`: Django consulta los datos del usuario con el encabezado `X-Internal-Api-Key`.

Si el login acepta las credenciales pero Django muestra que no puede cargar el perfil, comprueba que la clave interna de desarrollo coincida en `Backend-django/config/settings.py` y `Backend-spring/src/main/resources/application.properties`, y que `SPRING_API_BASE_URL` apunte al puerto donde está Spring.


## 4. Configurar y arrancar el frontend React

El frontend web incluye registro, inicio de sesión, consulta básica del perfil y cierre de sesión. Está desarrollado con React y Vite, siguiendo el diseño de Figma del equipo, con adaptación a dispositivos móviles y animaciones suaves.

### Requisitos adicionales

- Node.js y npm.
- MySQL, Spring Boot y Django ejecutándose según los pasos anteriores.

### Iniciar React

Abre una nueva terminal en `Frontend-web/react`.

En la primera ejecución, instala las dependencias:

```powershell
npm install
```

Inicia el servidor de desarrollo:

```powershell
npm run dev
```

Abre **http://127.0.0.1:5173/login**.

| Servicio | Dirección local |
|---|---|
| Frontend React | http://127.0.0.1:5173 |
| Backend Django | http://127.0.0.1:8000 |
| Servicio Spring Boot | http://127.0.0.1:8080 |

El puerto `8000` conserva las páginas provisionales de Django. Para utilizar la nueva interfaz, entra al puerto `5173`.

React envía las solicitudes a Django mediante el proxy de Vite. Django administra la sesión y se comunica con Spring Boot, que consulta y guarda las cuentas en MySQL. La clave interna permanece en los servidores.

### Reglas de registro e inicio de sesión

- Solo se aceptan correos con el dominio exacto `@tecsup.edu.pe`.
- El registro solicita nombre, apellido, fecha de nacimiento, correo institucional, contraseña y confirmación.
- La fecha de nacimiento debe ser anterior al día actual. No se establece una edad mínima.
- La contraseña del registro debe contener entre 8 y 128 caracteres.
- Las contraseñas deben coincidir y el correo no debe estar registrado.

Estas reglas se validan en la interfaz y en el backend.

La columna `fecha_nacimiento` ya existe en el esquema. Si la base actual utiliza ese esquema, **no es necesario eliminarla ni volver a importarla**. Las cuentas anteriores pueden conservar ese campo vacío.

Después de actualizar el backend Java, reinicia Spring Boot para cargar los cambios.

### API web de Django

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/web/csrf/` | Obtener el token CSRF del formulario. |
| POST | `/api/web/registro/` | Registrar la cuenta mediante Spring. |
| POST | `/api/web/login/` | Validar las credenciales y crear la sesión web. |
| GET | `/api/web/perfil/` | Consultar el perfil de la sesión actual. |
| POST | `/api/web/logout/` | Cerrar la sesión. |

Las operaciones de escritura requieren CSRF. Las contraseñas y los identificadores de sesión no se guardan en `localStorage`.

### Verificación

Desde `Frontend-web/react`:

```powershell
npm run build
npm run lint
```

Desde `Backend-django`:

```powershell
.\.venv\Scripts\python.exe manage.py test cuentas
```

Desde `Backend-spring`, con Java 21:

```powershell
.\mvnw.cmd test
```

## 5. Configurar y arrancar la aplicación móvil

La app Android (`Mobile/`) se conecta **directamente a Spring Boot**, sin pasar por Django:

| Pantalla | Endpoint de Spring |
|---|---|
| Registro | `POST /api/auth/registro` |
| Inicio de sesión | `POST /api/auth/login` |

Tras el login, la app guarda en el teléfono solo los datos de perfil que devuelve Spring (nunca la contraseña) y muestra el estado de la cuenta: `PENDIENTE` hasta que un administrador la apruebe, luego `ACTIVO`. Cerrar sesión borra esos datos. La app no usa la clave interna `X-Internal-Api-Key`, porque esa clave no debe viajar dentro de un APK.

### Pasos

1. Arranca MySQL y Spring Boot como en los pasos 1 y 2.
2. Abre la carpeta `Mobile` en Android Studio y espera a que termine la sincronización de Gradle.
3. Ejecuta la app en un emulador. Por defecto apunta a `http://10.0.2.2:8080/`, que es el `localhost` de la PC visto desde el emulador.
4. En un celular físico conectado a la misma red Wi-Fi, usa la IP de tu PC (por ejemplo, `ipconfig` en Windows) y pásala al compilar:

```powershell
.\gradlew.bat installDebug -Pyourpeak.apiUrl=http://192.168.1.50:8080/
```

También puedes dejarla fija en `Mobile/gradle.properties` con `yourpeak.apiUrl=http://192.168.1.50:8080/`. Si Windows pregunta, permite que Java acepte conexiones en el firewall para el puerto de Spring.

La compilación debug permite HTTP sin cifrar (`app/src/debug/res/xml/network_security_config.xml`) solo para desarrollo local.

En Android 17 (API 37) la app debug pide al abrirse el permiso para conectarse a dispositivos de la red local. **Acéptalo**: sin él, Android bloquea la conexión con Spring en la PC y la app muestra que no hay conexión. Si lo rechazaste, actívalo en Ajustes → Apps → YourPeak → Permisos, o reinstala la app.

### Pruebas de la app

Desde `Mobile`:

```powershell
.\gradlew.bat testDebugUnitTest
```

Comprueban las validaciones (mismas reglas que Spring) y cómo la app interpreta las respuestas de Spring (login correcto, 401, 400 con errores por campo y servidor apagado).

### Funcionalidades pendientes

- Recuperación de contraseña.
- Autenticación con Google.
- Verificación de propiedad del correo institucional.

La restricción del dominio comprueba el formato permitido; todavía no verifica que la persona tenga acceso a ese correo.

El proxy de Vite se utiliza durante el desarrollo. Para publicar la web, configura un servidor que sirva la compilación de React y reenvíe `/api/web/` a Django bajo el mismo origen.
