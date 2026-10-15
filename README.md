# YourPeak — Guía de ejecución

Orden de inicio: **MySQL en XAMPP → Spring Boot en IntelliJ → aplicación Android**.

## 1. Descargar el proyecto

Clonar el repositorio con GitHub Desktop o descargarlo desde **Code → Download ZIP** y extraerlo. Si ya está clonado, obtener los últimos cambios con **Fetch origin / Pull origin**.

- `Backend-usuario`: abrir en IntelliJ.
- `FrontEnd-usuario`: abrir en Android Studio.

Tener instalados XAMPP, SQLyog, JDK 21, IntelliJ y Android Studio con los componentes del SDK que solicite el proyecto.

## 2. Preparar la base de datos (primera vez)

1. Abrir XAMPP e iniciar **MySQL**.
2. Abrir SQLyog y conectarse al servidor local con las credenciales de administrador de esa computadora.
3. Para una instalación nueva, ejecutar por completo los archivos de `Backend-usuario/database` en este orden:
   - `01_estructura.sql`
   - `02_catalogos.sql`
4. Aplicar después cualquier script adicional de actualización incluido por el equipo, siguiendo su orden indicado.

Si la base ya está preparada, omitir la importación inicial.

## 3. Configurar el usuario de conexión (primera vez)

Desde SQLyog, como administrador, crear la cuenta si todavía no existe. Reemplazar `ELIGE_TU_CLAVE_LOCAL` por una contraseña propia, dentro de las comillas simples:

```sql
CREATE USER 'yourpeak_app'@'localhost'
IDENTIFIED BY 'ELIGE_TU_CLAVE_LOCAL';

GRANT SELECT, INSERT ON yourpeak_db.usuario
TO 'yourpeak_app'@'localhost';

GRANT SELECT, INSERT ON yourpeak_db.cliente
TO 'yourpeak_app'@'localhost';

GRANT SELECT ON yourpeak_db.tipo_estado
TO 'yourpeak_app'@'localhost';
```

Estos permisos corresponden al registro de usuarios. Si la cuenta ya está configurada, omitir este paso.

## 4. Abrir y ejecutar el backend

1. En IntelliJ, seleccionar **Open** y abrir `Backend-usuario`, donde está `pom.xml`.
2. Esperar a que termine la importación de Maven.
3. Seleccionar **JDK 21** para el proyecto y su ejecución.
4. Abrir `YourpeakBackendApplication.java` y preparar su configuración de ejecución.
5. En **Run → Edit Configurations**, seleccionar esa configuración y entrar en **Modify options → Environment variables**.
6. Agregar la misma contraseña elegida en el paso anterior, sin comillas:

```text
YOURPEAK_DB_PASSWORD=ELIGE_TU_CLAVE_LOCAL
```

Mantener en `application.properties`:

```properties
server.port=8080
spring.datasource.url=jdbc:mariadb://localhost:3306/yourpeak_db
spring.datasource.username=yourpeak_app
spring.datasource.password=${YOURPEAK_DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

Si MySQL usa otro puerto en esa computadora, ajustar el puerto de la URL. No subir la contraseña real al repositorio.

7. Ejecutar `YourpeakBackendApplication` y esperar los mensajes:

```text
Tomcat started on port 8080
Started YourpeakBackendApplication
```

Dejar Spring Boot ejecutándose mientras se prueba Android.

## 5. Abrir Android y elegir la conexión

1. En Android Studio, seleccionar **Open** y abrir `FrontEnd-usuario`, donde está `settings.gradle.kts`.
2. Esperar la sincronización de Gradle e instalar los componentes del SDK solicitados.
3. Buscar `ApiClient.kt` pulsando **Shift dos veces**. Está en:

```text
app/src/main/java/com/frontendusuario/app/data/remote/ApiClient.kt
```

### Con emulador

El backend debe ejecutarse en la misma laptop que el emulador. Configurar:

```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/"
```

1. Abrir **Device Manager** y crear un dispositivo virtual si todavía no existe.
2. Iniciar el emulador y esperar su pantalla de inicio.
3. Seleccionarlo en la barra superior de Android Studio.
4. Seleccionar la configuración **app**, variante **debug**, y pulsar **Run**.

Con esta dirección no se necesita ejecutar `adb reverse`.

### Con celular por USB

Configurar:

```kotlin
private const val BASE_URL = "http://127.0.0.1:8080/"
```

1. Activar las opciones de desarrollador y la depuración USB del celular.
2. Conectarlo a la laptop y aceptar la autorización de depuración.
3. En la terminal PowerShell de Android Studio, ejecutar:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -d reverse tcp:8080 tcp:8080
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -d reverse --list
```

Si el SDK está instalado en otra ubicación, usar su ruta real. La lista debe mostrar `tcp:8080 tcp:8080`.

4. Seleccionar el celular, la configuración **app** y la variante **debug**. Pulsar **Run**.

Repetir la redirección después de reconectar o reiniciar el dispositivo si se perdió. Después de cambiar `BASE_URL`, volver a ejecutar la app.

## 6. Probar el registro

Completar el formulario con datos de prueba y un correo nuevo, por ejemplo:

```text
Nombre: Carlos
Apellido: Prueba
Correo: carlos.prueba@example.com
Contraseña: Mi primera prueba desde Android!
```

Pulsar **Crear cuenta**. El resultado esperado es:

> Registro exitoso. Tu cuenta está pendiente de aprobación.

Para comprobarlo en SQLyog:

```sql
SELECT u.id_usuario, u.nombre, u.apellido, u.correo,
       c.id_cliente, e.nombre AS estado
FROM yourpeak_db.usuario u
JOIN yourpeak_db.cliente c ON c.id_usuario = u.id_usuario
JOIN yourpeak_db.tipo_estado e ON e.id_tipo_estado = c.id_tipo_estado
WHERE u.correo = 'carlos.prueba@example.com';
```

## 7. Ejecutar el proyecto los siguientes días

1. Iniciar **MySQL** desde XAMPP.
2. Ejecutar **Spring Boot** desde IntelliJ y esperar a que arranque.
3. Iniciar el emulador o conectar el celular por USB y preparar su redirección.
4. Ejecutar **app** desde Android Studio.

No repetir la importación inicial de SQL ni la creación del usuario. SQLyog solo es necesario para consultar datos; no necesita permanecer abierto.
