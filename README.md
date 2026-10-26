# YourPeak

## Contexto del proyecto

YourPeak es una plataforma de gestión y seguimiento de entrenamiento para un gimnasio. La idea general contempla cuentas de clientes, membresías, registro físico, disponibilidad, rutinas, ejercicios y sesiones de entrenamiento. El esquema SQL que está ahora en el repositorio cubre solo una parte de esa base; no representa todavía todas las áreas del diseño.

La aplicación web utiliza React como interfaz y Django como backend web, además de la aplicación móvil Android. Spring Boot ofrece los servicios de cuenta y conserva los datos de usuario en MySQL. La base de Django es SQLite y se usa para la sesión web; no es una copia de MySQL ni debe almacenar las cuentas de usuario.

## Arquitectura y estado actual

| Parte | Tecnología | Estado |
|---|---|---|
| Servicio de cuentas | Spring Boot, Java 21 y MySQL | Ofrece registro, login, rol y estado de cuenta, además de consulta y aprobación de clientes pendientes. Usa `yourpeak_db`; la conexión se configura en `Backend-spring/src/main/resources/application.properties`. |
| Web/backend | Django y SQLite | Expone la API web para React, valida el rol administrador contra Spring en cada operación administrativa y conserva solo el ID de usuario en la sesión SQLite. Mantiene los formularios provisionales. |
| Web/interfaz final | React | Registro, login, estado de aprobación, perfil básico y panel administrativo de usuarios. Atleta y logo separados; textos, fondos, figuras y distribución construidos con código. |
| Aplicación móvil | Android, Kotlin y Jetpack Compose | Está creado el proyecto inicial; las funciones del gimnasio y la conexión completa con los servicios están pendientes. |
| Base de datos | MySQL/MariaDB | El esquema completo y sus catálogos iniciales están en `Backend-spring/database/yourpeak_schema.sql`. No contiene cuentas reales. |

### Recorrido actual de login

1. La persona completa el formulario de React. React envía la solicitud a Django mediante `/api/web/login/`, con la protección CSRF correspondiente.
2. Django reenvía correo y contraseña a Spring Boot mediante `POST /api/auth/login`; Django no comprueba contraseñas ni busca cuentas en SQLite.
3. Spring Boot valida la contraseña y consulta la cuenta en MySQL. El registro funciona igual: Django envía los datos a `POST /api/auth/registro` y Spring crea el usuario y su perfil de cliente en MySQL.
4. Si el login es correcto, Django guarda solo el ID de usuario en su sesión SQLite. Al cerrar sesión, Django borra esa sesión.
5. Para mostrar el perfil, Django pide `GET /api/auth/usuarios/{idUsuario}` a Spring usando el encabezado `X-Internal-Api-Key`.
6. React usa el rol y el estado devueltos por Spring para mostrar el panel administrativo, la pantalla de espera o la cuenta activa. Iniciar sesión no cambia el estado PENDIENTE.

Por tanto, MySQL es la fuente de verdad para las cuentas. SQLite guarda los datos de sesión propios de Django y no es una copia de MySQL. Django y Spring comparten una clave interna de desarrollo definida en sus archivos de configuración para que Django pueda consultar el perfil y comunicarse con los endpoints internos.

## Requisitos

- Java 21.
- MySQL o MariaDB en ejecución.
- Python 3.12, 3.13 o 3.14 (versiones compatibles con la versión de Django fijada en `Backend-django/requirements.txt`).
- Git.
- Node.js y npm para ejecutar el frontend React. Se verificó localmente con Node.js 24 y npm 11.

## 1. Preparar MySQL para Spring Boot

1. Inicia el servicio local de MySQL/MariaDB.
2. En SQLyog o MySQL Workbench abre y ejecuta `Backend-spring/database/yourpeak_schema.sql`. El archivo crea `yourpeak_db`, las tablas y los catálogos iniciales.
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

El frontend web incluye registro, inicio de sesión, estado de aprobación, perfil básico, cierre de sesión y panel administrativo de usuarios. Está desarrollado con React y Vite, siguiendo el estilo de Figma del equipo. El atleta y el logo son recursos separados; los textos, el fondo y la figura roja se construyen con HTML y CSS para ajustar correctamente la distribución. Conserva animaciones suaves y respeta la preferencia de movimiento reducido. Esta etapa corresponde a la web; no modifica la aplicación Android de `Mobile`.

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
| GET | `/api/web/admin/clientes/pendientes/` | Listar clientes pendientes; requiere administrador. |
| GET | `/api/web/admin/clientes/pendientes/{idCliente}/` | Consultar el detalle de un cliente pendiente; requiere administrador. |
| POST | `/api/web/admin/clientes/{idCliente}/aprobar/` | Aprobar un cliente pendiente; requiere administrador y CSRF. |

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

### Funcionalidades pendientes

- Recuperación de contraseña.
- Autenticación con Google.
- Verificación de propiedad del correo institucional.

La restricción del dominio comprueba el formato permitido; todavía no verifica que la persona tenga acceso a ese correo.

El proxy de Vite se utiliza durante el desarrollo. Para publicar la web, configura un servidor que sirva la compilación de React y reenvíe `/api/web/` a Django bajo el mismo origen.


## 5. Panel administrativo y aprobación de usuarios

### Acceso según rol y estado

Todos utilizan el mismo login: **http://127.0.0.1:5173/login**.

| Cuenta | Pantalla mostrada |
|---|---|
| Administrador | Panel de usuarios en `/admin/usuarios`. |
| Cliente PENDIENTE | Pantalla de espera en `/pendiente`, con opción de consultar su estado y cerrar sesión. |
| Cliente ACTIVO | Pantalla básica de su cuenta en `/`. |
| Cliente INACTIVO o sin acceso habilitado | Mensaje para consultar con la administración. |

El registro público siempre crea una cuenta cliente PENDIENTE. El login permite comprobar las credenciales y consultar ese estado, pero no activa la cuenta. El cliente deja de ver la pantalla de espera después de que un administrador lo aprueba y vuelve a consultar su estado o inicia sesión de nuevo.

Entrar directamente a `/admin/usuarios` no concede permisos. Django obtiene el ID del usuario desde su sesión y comprueba el rol con Spring; Spring también valida al administrador antes de listar, consultar o aprobar. React no envía la clave interna ni decide quién es administrador.

### Preparar el primer administrador local

Las cuentas y sus roles pertenecen a la base MySQL de cada computadora: descargar el repositorio no copia los usuarios de otro integrante.

1. Crea una cuenta de prueba desde el registro web y conserva su contraseña.
2. En SQLyog o MySQL Workbench, asigna a esa cuenta el rol administrador. Sustituye `CORREO_ADMIN_LOCAL@tecsup.edu.pe` por el correo exacto de la cuenta que administrará esta instalación local:

```sql
INSERT INTO yourpeak_db.administrador (id_usuario)
SELECT u.id_usuario
FROM yourpeak_db.usuario u
WHERE u.correo = 'CORREO_ADMIN_LOCAL@tecsup.edu.pe'
  AND NOT EXISTS (
    SELECT 1
    FROM yourpeak_db.administrador a
    WHERE a.id_usuario = u.id_usuario
  );
```

3. Comprueba la asignación:

```sql
SELECT u.id_usuario, u.nombre, u.correo
FROM yourpeak_db.administrador a
JOIN yourpeak_db.usuario u ON u.id_usuario = a.id_usuario;
```

4. Cierra cualquier sesión abierta e inicia sesión con esa cuenta en el login de React. Se abrirá el panel administrativo.

Este procedimiento es para preparar el entorno local de desarrollo. El formulario público no permite elegir el rol administrador. Aunque la cuenta conserve el perfil de cliente creado durante el registro, el rol administrador tiene prioridad: queda excluida de la lista de pendientes y el backend rechaza aprobarla como cliente.

### Funciones disponibles en el panel

- Consultar las solicitudes pendientes reales de MySQL.
- Buscar por nombre, correo o número de solicitud.
- Ordenar por antigüedad o nombre y recorrer las páginas de resultados.
- Revisar nombre, correo, fecha de nacimiento y fecha de registro antes de aprobar.
- Confirmar la aprobación: el estado cambia de PENDIENTE a ACTIVO y la solicitud desaparece de la lista.
- Actualizar la lista y manejar estados vacíos, errores de conexión o solicitudes ya atendidas por otro administrador.

Solo el apartado **Usuarios** está habilitado. Resumen, Entrenamientos y Membresías aparecen como próximos módulos. No se implementó rechazo de solicitudes porque el backend actual solo ofrece aprobación.

### Prueba completa del flujo

1. Mantén MySQL, Spring Boot, Django y React ejecutándose.
2. Registra un cliente distinto de la cuenta administradora.
3. Inicia sesión como cliente: debe aparecer PENDIENTE de aprobación.
4. Cierra sesión y entra como administrador.
5. En Usuarios, busca al cliente, pulsa Revisar y confirma Aprobar usuario.
6. Cierra la sesión administrativa y entra de nuevo como cliente: debe mostrarse su cuenta activa.

Para confirmar el estado guardado, sustituye el correo en esta consulta:

```sql
SELECT u.nombre, u.apellido, u.correo, e.nombre AS estado
FROM yourpeak_db.usuario u
JOIN yourpeak_db.cliente c ON c.id_usuario = u.id_usuario
JOIN yourpeak_db.tipo_estado e ON e.id_tipo_estado = c.id_tipo_estado
WHERE u.correo = 'CORREO_CLIENTE_LOCAL@tecsup.edu.pe';
```

El resultado debe mostrar ACTIVO. No es necesario borrar la base ni volver a importar el esquema para esta etapa: las tablas de roles y estados ya existen.

### Ejecución y problemas frecuentes

- Orden recomendado: MySQL → Spring Boot (`8080`) → Django (`8000`) → React (`5173`). SQLyog es opcional para consultar datos; no necesita permanecer abierto.
- Abre la carpeta clonada que contiene esta versión del código. Una copia anterior descargada como ZIP no se actualiza al hacer pull en otra carpeta.
- Si aparece un error al entrar al login sin enviar el formulario, comprueba los servidores: React consulta automáticamente la sesión al cargar y la petición puede fallar si Django no está disponible.
- Si el panel no abre, verifica el rol de esa cuenta en tu base local y vuelve a iniciar sesión.
- Si una solicitud ya fue aprobada, actualiza la lista. El backend rechaza repetir la aprobación con HTTP 409.
- Reinicia Spring después de actualizar el código Java. Django normalmente se recarga al cambiar sus archivos; Vite actualiza la interfaz durante el desarrollo.

### Verificaciones realizadas en esta etapa

- Compilación y análisis del frontend sin errores (`npm run build` y `npm run lint`).
- 16 pruebas de Django aprobadas, incluidas validación de rol, identidad tomada de la sesión y protección CSRF.
- 12 pruebas de Spring aprobadas, incluidas exclusión de administradores y rechazo de su aprobación.
- Pruebas del navegador con datos simulados para búsqueda, paginación, detalle, cancelación, aprobación, conflictos y estados de acceso.
- Prueba manual contra MySQL local: registro de cliente, espera, aprobación administrativa y acceso posterior como cliente activo.

Las pruebas automáticas de Spring utilizan H2 y las de Django utilizan una base de pruebas; no cambian las cuentas reales de MySQL. En un equipo con poca memoria se verificó Spring con memoria limitada y `-DforkCount=0`.
