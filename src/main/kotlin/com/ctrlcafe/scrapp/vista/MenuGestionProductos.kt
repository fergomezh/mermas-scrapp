package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.controlador.LoteController
import com.ctrlcafe.scrapp.controlador.ProductoController
import com.ctrlcafe.scrapp.modelo.Producto
import com.ctrlcafe.scrapp.servicio.MotorSemaforo
import com.ctrlcafe.scrapp.util.Logger
import com.ctrlcafe.scrapp.util.Validador

/**
 * Submenú de administración de productos (CRUD).
 *
 * Toda operación pasa por `ProductoController`, que es quien verifica
 * `Accion.ADMINISTRAR_PRODUCTOS` y aplica las reglas de negocio.
 */
class MenuGestionProductos(
    private val productoController: ProductoController,
    private val loteController: LoteController,
    private val semaforo: MotorSemaforo
) {
    fun mostrar() {
        while (true) {
            ConsolaUI.limpiar()
            ConsolaUI.titulo("Gestión de productos")
            println("1. Listar productos")
            println("2. Ver detalle de un producto")
            println("3. Crear producto")
            println("4. Editar producto")
            println("5. Eliminar producto")
            println("0. Volver")

            when (Validador.leerOpcion("Seleccione una opción: ", 0..5)) {
                1 -> protegido(JAVA) { ConsolaUI.mostrarProductos(productoController.listarProductos()) }
                2 -> protegido(JAVA) { verDetalle() }
                3 -> protegido(JAVA, "No se pudo crear el producto") { crear() }
                4 -> protegido(JAVA, "No se pudo actualizar el producto") { editar() }
                5 -> protegido(JAVA, "No se pudo eliminar el producto") { eliminar() }
                0 -> return
            }
            ConsolaUI.pausa()
        }
    }

    private fun verDetalle() {
        val producto = elegirProducto("Seleccione el producto a consultar") ?: return cancelado()
        ConsolaUI.mostrarProductoDetalle(producto)

        // listarLotesPorProducto + el semáforo dan el estado real de cada lote del producto.
        val estados = loteController.listarLotesPorProducto(producto.id).map { semaforo.recalcular(it) }
        if (estados.isEmpty()) {
            println("\nEste producto no tiene lotes registrados.")
        } else {
            ConsolaUI.mostrarLotes(estados.sortedBy { it.lote.fechaCaducidad })
        }
    }

    private fun crear() {
        val id = siguienteId(productoController.listarProductos().map(Producto::id), PREFIJO_ID, DIGITOS_ID)
        println("\nNuevo producto (ID asignado: $id)")
        ConsolaUI.separador()

        val nombre = Validador.leerTexto("Nombre: ")
        val categoria = Validador.leerTexto("Categoría: ")
        val costo = Validador.leerDecimal("Costo unitario: ", minimo = 0.0)
        val precio = Validador.leerDecimal("Precio de venta: ", minimo = 0.0)
        val unidad = Validador.leerTexto("Unidad de medida (ej. Unidad, Litro, Bolsa): ")

        if (precio < costo) {
            println("\nAviso: el precio de venta es menor al costo; el producto quedaría con margen negativo.")
        }

        println("\nSe creará: $nombre | $categoria | costo ${ConsolaUI.moneda(costo)} | precio ${ConsolaUI.moneda(precio)} | $unidad")
        if (!confirmar()) return cancelado()

        val producto = productoController.crearProducto(id, nombre, categoria, costo, precio, unidad)
        Logger.info(JAVA, "Producto ${producto.id} creado")
        println("\nProducto ${producto.id} creado correctamente.")
    }

    private fun editar() {
        val actual = elegirProducto("Seleccione el producto a editar") ?: return cancelado()
        ConsolaUI.mostrarProductoDetalle(actual)
        println("\nDeje vacío cualquier campo de texto para conservar el valor actual.")

        val nombre = Validador.leerTexto("Nombre [${actual.nombre}]: ", permitirVacio = true)
            .ifBlank { actual.nombre }
        val categoria = Validador.leerTexto("Categoría [${actual.categoria}]: ", permitirVacio = true)
            .ifBlank { actual.categoria }
        val costo = Validador.leerDecimal("Costo unitario [${actual.costoUnitario}]: ", minimo = 0.0)
        val precio = Validador.leerDecimal("Precio de venta [${actual.precioVenta}]: ", minimo = 0.0)
        val unidad = Validador.leerTexto("Unidad de medida [${actual.unidadMedida}]: ", permitirVacio = true)
            .ifBlank { actual.unidadMedida }

        if (costo != actual.costoUnitario) {
            println("\nNota: las mermas ya registradas conservan su costo congelado; solo cambian las futuras.")
        }
        if (!confirmar()) return cancelado()

        val producto = productoController.actualizarProducto(actual.id, nombre, categoria, costo, precio, unidad)
        Logger.info(JAVA, "Producto ${producto.id} actualizado")
        println("\nProducto ${producto.id} actualizado correctamente.")
    }

    private fun eliminar() {
        val producto = elegirProducto("Seleccione el producto a eliminar") ?: return cancelado()
        ConsolaUI.mostrarProductoDetalle(producto)

        println("\nEsta acción no se puede deshacer.")
        if (!confirmar()) return cancelado()

        // El controlador rechaza el borrado si quedan lotes o mermas asociadas.
        productoController.eliminarProducto(producto.id)
        Logger.info(JAVA, "Producto ${producto.id} eliminado")
        println("\nProducto ${producto.id} eliminado.")
    }

    private fun elegirProducto(encabezado: String): Producto? =
        elegirDeLista(
            encabezado,
            productoController.listarProductos(),
            "No hay productos registrados."
        ) { "%-10s %-30s %-18s %10s".format(it.id, it.nombre.take(30), it.categoria.take(18), ConsolaUI.moneda(it.costoUnitario)) }

    companion object {
        private val JAVA = MenuGestionProductos::class.java
        private const val PREFIJO_ID = "PROD-"
        private const val DIGITOS_ID = 2
    }
}
