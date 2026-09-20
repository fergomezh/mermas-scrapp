# Scrapp - Control de Mermas

Proyecto Kotlin/JVM para el control de mermas de CTRL+CAFE.

* Integrantes:

- Victor Emmanuel Velasco Martínez  VM251307
- Fernando José Gómez Hernández     GH251230
- José Eduardo Aquino Medrano       AM252078
- William Eduardo Montano Aguilar   MA251192


## Ejecución en IntelliJ IDEA

1. Abrir el proyecto como proyecto Gradle.
2. Esperar la sincronización de Gradle.
3. Ejecutar `MainKt` o la tarea Gradle `run`.
4. La aplicación inicia en consola.

La tarea `run` conecta la entrada estándar y fija UTF-8 en la salida. Al ejecutar `MainKt`
directamente conviene añadir `-Dstdout.encoding=UTF-8` en las VM options de la configuración,
o los acentos se verán como recuadros en Windows.

## Credenciales de prueba

- Administrador: `admin` / `admin123`
- Operativo: `barista` / `barista123`

El usuario no distingue mayúsculas: `admin` y `Admin` son la misma cuenta.

## Funcionalidades

| Opción | Operativo | Administrador |
|---|:---:|:---:|
| Ver productos | ✔ | ✔ |
| Monitor de lotes con semáforo de caducidad | ✔ | ✔ |
| Ver mermas registradas | ✔ | ✔ |
| Registrar merma | ✔ | ✔ |
| Resumen / reporte | | ✔ |
| Gestionar productos | | ✔ |
| Gestionar lotes | | ✔ |
| Gestionar mermas | | ✔ |

Los menús no leen de los repositorios: toda operación pasa por un controlador, que verifica
la acción contra el rol del usuario en sesión antes de ejecutarla.

### Registro de mermas

1. Se elige el lote de una lista ordenada por urgencia; los vencidos aparecen primero.
2. Se indican la cantidad, la causa y, opcionalmente, la evidencia. La cantidad se acota al
   stock disponible del lote en el momento de pedirla.
3. Tras confirmar, `MermaController` congela el costo unitario y descuenta el stock del lote.
4. `OrquestadorMerma` recalcula y la consola muestra el antes y el después: stock y estado del lote,
   pérdida del día, tasa de merma y producción sugerida para mañana.

### Semáforo de caducidad

Lo calcula `MotorSemaforo` con las horas restantes hasta el final del día de caducidad:
VERDE (más de 72 h), AMARILLO (24 a 72 h), ROJO (0 a 24 h) y NEGRO (vencido).

### Resumen / reporte

Cubre los últimos 30 días: pérdida y costo de producción del periodo, índice de merma,
top 5 de productos críticos y producción sugerida para el día siguiente por producto.
Se exporta a `reportes/resumen_AAAA-MM-DD.txt`.

### Gestión de productos

Consulta, alta, edición y baja con `ProductoController`. Las operaciones de escritura exigen la
acción `ADMINISTRAR_PRODUCTOS`. El ID se asigna solo a partir del mayor existente (`PROD-08`,
`PROD-09`...). El detalle de un producto muestra además sus lotes con el estado del semáforo.

Cambiar el costo de un producto no altera las mermas ya registradas, porque cada una conserva su
costo congelado. No se puede eliminar un producto que todavía tenga lotes o mermas asociadas:
quedarían huérfanos y el monitor mostraría el ID en lugar del nombre.

### Gestión de lotes

Consulta, alta y descuento de existencias con `LoteController`. Las operaciones de escritura
exigen la acción `ADMINISTRAR_LOTES`. El ID también es automático (`LOT-011`, `LOT-012`...) y la
caducidad sale de la vida útil o se indica a mano. El detalle de un lote incluye el historial de
mermas que se le han cargado.

El descuento de existencias cubre el consumo o la venta normal; `MotorSemaforo` valida antes que
el lote no esté vencido. Para producto que se perdió está el registro de mermas, que es el único
camino que deja descontar de un lote vencido.

### Gestión de mermas

Consulta, edición y borrado de mermas ya registradas con `MermaController`. Editar y eliminar
exigen la acción `ADMINISTRAR_MERMAS`, distinta de la de registrar. Editar ajusta el stock del
lote por la diferencia y eliminar devuelve las unidades al lote. El detalle muestra los campos
que la tabla resumida no alcanza a listar: fecha, usuario que la registró, evidencia y costo
unitario congelado.

## Estructura

- `modelo/`: entidades y reglas de dominio.
- `repositorio/`: colecciones en memoria y datos iniciales.
- `controlador/`: autenticación, permisos por rol y gestión de productos, lotes y mermas.
- `servicio/`: motores de semáforo, financiero y proyección, y el orquestador de recálculo.
- `vista/`: interfaz de consola.
- `util/`: validaciones, excepciones y registro de errores.

Los datos viven en memoria: al cerrar la aplicación se pierden. Los errores se registran en `logs/errores.log`.
