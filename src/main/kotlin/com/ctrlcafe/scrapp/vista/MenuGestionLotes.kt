package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.controlador.LoteController
import com.ctrlcafe.scrapp.controlador.ProductoController
import com.ctrlcafe.scrapp.modelo.Lote
import com.ctrlcafe.scrapp.modelo.Producto
import com.ctrlcafe.scrapp.repositorio.MermaRepositorio
import com.ctrlcafe.scrapp.servicio.MotorSemaforo
import com.ctrlcafe.scrapp.util.Logger
import com.ctrlcafe.scrapp.util.Validador
import java.time.LocalDate

/**
 * Submenú de administración de lotes: alta de inventario y descuento por
 * consumo normal (no por merma; para eso está el flujo de registro de mermas).
 *
 * `LoteController` verifica `Accion.ADMINISTRAR_LOTES` y delega la validación
 * de caducidad a `MotorSemaforo.verificarUsable`.
 */
class MenuGestionLotes(
    private val loteController: LoteController,
    private val productoController: ProductoController,
    private val mermaRepositorio: MermaRepositorio,
    private val semaforo: MotorSemaforo
) {
    fun mostrar() {
        while (true) {
            ConsolaUI.limpiar()
            ConsolaUI.titulo("Gestión de lotes")
            println("1. Listar lotes")
            println("2. Ver detalle de un lote")
            println("3. Registrar lote nuevo")
            println("4. Descontar existencias (consumo o venta)")
            println("0. Volver")

            when (Validador.leerOpcion("Seleccione una opción: ", 0..4)) {
                1 -> protegido(JAVA) {
                    ConsolaUI.mostrarLotes(semaforo.recalcularTodos(incluirAgotados = true))
                }
                2 -> protegido(JAVA) { verDetalle() }
                3 -> protegido(JAVA, "No se pudo registrar el lote") { registrar() }
                4 -> protegido(JAVA, "No se pudo descontar el stock") { descontar() }
                0 -> return
            }
            ConsolaUI.pausa()
        }
    }

    private fun verDetalle() {
        val lote = elegirLote("Seleccione el lote a consultar", loteController.listarLotes()) ?: return cancelado()
        // listarPorLote trae el historial de mermas de ese lote concreto.
        ConsolaUI.mostrarLoteDetalle(semaforo.recalcular(lote), mermaRepositorio.listarPorLote(lote.id))
    }

    private fun registrar() {
        val producto = elegirDeLista(
            "Seleccione el producto del lote",
            productoController.listarProductos(),
            "No hay productos registrados; cree uno antes de registrar lotes."
        ) { "%-10s %-30s %-18s %10s".format(it.id, it.nombre.take(30), it.categoria.take(18), ConsolaUI.moneda(it.costoUnitario)) }
            ?: return cancelado()

        val id = siguienteId(loteController.listarLotes().map(Lote::id), PREFIJO_ID, DIGITOS_ID)
        println("\nNuevo lote (ID asignado: $id) para ${producto.nombre}")
        ConsolaUI.separador()

        val cantidad = Validador.leerDecimal("Cantidad inicial: ", minimo = 0.01)
        val costo = Validador.leerDecimal(
            "Costo unitario del lote (costo actual del producto: ${ConsolaUI.moneda(producto.costoUnitario)}): ",
            minimo = 0.0
        )
        val ingreso = Validador.leerFecha("Fecha de ingreso (AAAA-MM-DD, hoy es ${LocalDate.now()}): ")
        val vidaUtil = Validador.leerEntero("Vida útil en días: ", 0..MAX_VIDA_UTIL)

        println("\nCaducidad por vida útil: ${ConsolaUI.fecha(ingreso.plusDays(vidaUtil.toLong()))}")
        val caducidadManual =
            if (Validador.leerOpcion("¿Definir otra fecha de caducidad?  1. Sí  |  0. No: ", 0..1) == 1) {
                Validador.leerFecha("Fecha de caducidad (AAAA-MM-DD): ")
            } else null

        println("\nSe registrará: $id | ${producto.nombre} | %.2f unidades | ${ConsolaUI.moneda(costo)} c/u".format(cantidad))
        println("Caducidad: ${ConsolaUI.fecha(caducidadManual ?: ingreso.plusDays(vidaUtil.toLong()))}")
        if (!confirmar()) return cancelado()

        val lote = loteController.registrarLote(id, producto.id, cantidad, costo, ingreso, vidaUtil, caducidadManual)
        val estado = semaforo.recalcular(lote)
        Logger.info(JAVA, "Lote ${lote.id} registrado con %.2f unidades".format(cantidad))
        println("\nLote ${lote.id} registrado. Estado del semáforo: ${estado.estado.nivelAlerta} - ${estado.estado.descripcion}")
    }

    private fun descontar() {
        // Solo tiene sentido descontar de lotes que aún tengan existencias.
        val conStock = loteController.listarLotes().filter { it.cantidadDisponible > 0.0 }
        val lote = elegirLote("Seleccione el lote a descontar", conStock) ?: return cancelado()

        val antes = lote.cantidadDisponible
        val cantidad = Validador.leerDecimal(
            "\nCantidad a descontar (disponible: %.2f): ".format(antes),
            minimo = 0.01
        )
        println("\nSe descontarán %.2f unidades del lote ${lote.id}.".format(cantidad))
        println("Esto NO registra una merma: use 'Registrar merma' si el producto se perdió.")
        if (!confirmar()) return cancelado()

        // Si el lote está vencido, verificarUsable lanza LoteVencidoException.
        val actualizado = loteController.descontarExistencias(lote.id, cantidad)
        Logger.info(JAVA, "Lote ${lote.id} descontado en %.2f unidades".format(cantidad))
        println()
        ConsolaUI.mostrarAjusteLote(actualizado.id, antes, actualizado.cantidadDisponible)
    }

    private fun elegirLote(encabezado: String, lotes: List<Lote>): Lote? {
        val nombres = productoController.listarProductos().associateBy(Producto::id)
        return elegirDeLista(encabezado, lotes, "No hay lotes disponibles para esta operación.") {
            "%-10s %-26s %10.2f %12s".format(
                it.id, (nombres[it.productoId]?.nombre ?: it.productoId).take(26),
                it.cantidadDisponible, ConsolaUI.fecha(it.fechaCaducidad)
            )
        }
    }

    companion object {
        private val JAVA = MenuGestionLotes::class.java
        private const val PREFIJO_ID = "LOT-"
        private const val DIGITOS_ID = 3
        /** Tope defensivo para la vida útil; 10 años cubre cualquier insumo de cafetería. */
        private const val MAX_VIDA_UTIL = 3650
    }
}
