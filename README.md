# PSC01 - Proyecto de Gestión de Gastos

Aplicación Java con Spring Boot para gestionar grupos, gastos y pagos compartidos.

## Requisitos

- Java 21
- Gradle Wrapper (incluido en el repositorio)

No hace falta instalar Gradle globalmente porque se usa `gradlew`/`gradlew.bat`.

## Estructura del proyecto

Este repositorio usa una estructura multi-módulo con Gradle:

- `build.gradle` (raíz): configuración común para todos los módulos.
- `settings.gradle`: declara el módulo `myapp`.
- `myapp/`: aplicación principal Spring Boot.

Dentro de `myapp/src/main/java/com/mycompany/app/`:

- `App.java`: punto de entrada de la aplicación.
- `controller/`: controladores REST (`GastoController`, `GrupoController`, `PagoController`, `UsuarioController`).
- `service/`: lógica de negocio.
- `repository/`: acceso a datos con Spring Data JPA.
- `entity/`: entidades JPA.
- `dto/`: objetos de transferencia de datos.

Recursos relevantes:

- `myapp/src/main/resources/application.properties`: configuración de Spring y base de datos.
- `myapp/src/main/resources/data.sql`: datos iniciales.
- `myapp/src/main/resources/static/`: frontend estático (`index.html`, `dashboard.html`, `grupo.html`).

Tests:

- `myapp/src/test/java/...`: pruebas unitarias y de servicio.

## Cómo ejecutar la aplicación

En myapp\src\main\java\com\mycompany\app:

Ejecutar App.java

Desde la raíz del proyecto:

### En Windows

```powershell
.\gradlew.bat :myapp:bootRun
```

### En macOS/Linux

```bash
./gradlew :myapp:bootRun
```

Si todo va bien, Spring Boot arranca en el puerto por defecto `8080`.

Accesos útiles en local:

- Aplicación: `http://localhost:8080/`
- Front principal: `http://localhost:8080/index.html`
- Consola H2: `http://localhost:8080/h2-console`

Notas de base de datos:

- URL JDBC: `jdbc:h2:file:./myapp-data/testdb`
- Usuario: `sa`
- Contraseña: vacía

## Cómo ejecutar tests

### Ejecutar todos los tests del proyecto


Windows:

```powershell
.\gradlew.bat test
```

macOS/Linux:

```bash
./gradlew test
```

### Ejecutar solo tests del módulo `myapp`

Windows:

```powershell
.\gradlew.bat :myapp:test
```

macOS/Linux:

```bash
./gradlew :myapp:test
```

### Ejecutar una clase de test concreta

Windows:

```powershell
.\gradlew.bat :myapp:test --tests "com.mycompany.app.service.GastoServiceTest"
```

macOS/Linux:

```bash
./gradlew :myapp:test --tests "com.mycompany.app.service.GastoServiceTest"
```

## Build y limpieza

Generar artefactos:

Windows:

```powershell
.\gradlew.bat build
```

macOS/Linux:

```bash
./gradlew build
```

Limpiar artefactos:

Windows:

```powershell
.\gradlew.bat clean
```

macOS/Linux:

```bash
./gradlew clean
```

## Detalles adicionales

- La aplicación usa `spring.jpa.hibernate.ddl-auto=update`, por lo que las tablas se actualizan automáticamente al arrancar.
- Se inicializan datos con `data.sql` al inicio (`spring.sql.init.mode=always`).
- Los estáticos en `src/main/resources/static` se sirven directamente desde Spring Boot.

## Solución de problemas rápida

- Si el puerto `8080` está ocupado, cierra el proceso que lo use o configura otro puerto en `application.properties` (`server.port=...`).
- Si los tests fallan por caché o compilación anterior, prueba `clean test`.
- Si hay problemas de Java, verifica que estás usando Java 21.
