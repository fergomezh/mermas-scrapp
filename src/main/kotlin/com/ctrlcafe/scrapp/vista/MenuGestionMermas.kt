package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.controlador.LoteController
import com.ctrlcafe.scrapp.controlador.MermaController
import com.ctrlcafe.scrapp.controlador.ProductoController
import com.ctrlcafe.scrapp.modelo.CausaMerma
import com.ctrlcafe.scrapp.modelo.Merma
import com.ctrlcafe.scrapp.modelo.Producto
import com.ctrlcafe.scrapp.util.Logger
import com.ctrlcafe.scrapp.util.Validador

/**
 * Submenú de corrección de mermas ya registradas.
 *
 * `MermaController` verifica `Accion.ADMINISTRAR_MERMAS` y ajusta el stock del
 * lote: por la diferencia al editar, devolviendo las unidades al eliminar.
 */
class MenuGestionMermas(
    private val mermaController: MermaController,
    private val productoController: ProductoController,
    private val loteController: LoteController
) {
    fun mostrar() {
        while (true) {
            ConsolaUI.limpiar()
            ConsolaUI.titulo("Gestión de mermas")
            println("1. Listar mermas")
            println("2. Ver detalle de una merma")
            println("3. Editar merma")
            println("4. Eliminar merma")
            println("0. Volver")

            when (Validador.leerOpcion("Seleccione una opción: ", 0..4)) {
                1 -> protegido(JAVA) {
                    ConsolaUI.mostrarMermas(mermaController.listarMermas(), productoController.listarProductos())
                }
                2 -> protegido(JAVA) { verDetalle() }
                3 -> protegido(JAVA, "No se pudo actualizar la merma") { editar() }
                4 -> protegido(JAVA, "No se pudo eliminar la merma") { eliminar() }
                0 -> return
            }
            ConsolaUI.pausa()
        }
    }

    private fun verDetalle() {
        val merma = elegirMerma("Seleccione la merma a consultar") ?: return cancelado()
        // buscarMermaPorId relee del repositorio: confirma que el registro sigue vigente.
        val vigente = mermaController.buscarMermaPorId(merma.id)
        ConsolaUI.mostrarMermaDetalle(vigente, nombreDe(vigente.productoId))
    }

    private fun editar() {
        val actual = elegirMerma("Seleccione la merma a editar") ?: return cancelado()
        ConsolaUI.mostrarMermaDetalle(actual, nombreDe(actual.productoId))

        val lote = loteController.buscarLotePorId(actual.loteId)
        val disponible = lote.cantidadDisponible
        println("\nEl lote ${lote.id} tiene %.2f unidades disponibles.".format(disponible))
        println("Puede subir la cantidad hasta %.2f en total.".format(actual.cantidad + disponible))

        val cantidad = Validador.leerDecimal("\nNueva cantidad [%.2f]: ".format(actual.cantidad), minimo = 0.01)
        val causa = elegirCausa(actual.causa) ?: return cancelado()
        val evidencia = Validador.leerRutaOUrlImagen(
            "Nueva evidencia (ENTER para conservar '${actual.rutaEvidencia}'): ",
            permitirVacio = true
        ).ifBlank { actual.rutaEvidencia }
        val fecha = if (Validador.leerOpcion(
                "¿Cambiar la fecha (${ConsolaUI.fecha(actual.fecha)})?  1. Sí  |  0. No: ", 0..1
            ) == 1
        ) {
            Validador.leerFecha("Nueva fecha (AAAA-MM-DD): ")
        } else actual.fecha

        val diferencia = cantidad - actual.cantidad
        println("\nDiferencia sobre el lote: %+.2f unidades.".format(-diferencia))
        if (!confirmar()) return cancelado()

        val antes = lote.cantidadDisponible
        val actualizada = mermaController.actualizarMerma(actual.id, cantidad, causa, evidencia, fecha)
        Logger.info(JAVA, "Merma ${actualizada.id} actualizada a %.2f unidades".format(cantidad))

        println("\nMerma ${actualizada.id} actualizada.")
        ConsolaUI.mostrarAjusteLote(lote.id, antes, loteController.buscarLotePorId(lote.id).cantidadDisponible)
    }

    private fun eliminar() {
        val merma = elegirMerma("Seleccione la merma a eliminar") ?: return cancelado()
        ConsolaUI.mostrarMermaDetalle(merma, nombreDe(merma.productoId))

        val lote = loteController.buscarLotePorId(merma.loteId)
        val antes = lote.cantidadDisponible
        println("\nAl eliminarla, %.2f unidades volverán al lote ${lote.id}.".format(merma.cantidad))
        println("Esta acción no se puede deshacer.")
        if (!confirmar()) return cancelado()

        mermaController.eliminarMerma(merma.id)
        Logger.info(JAVA, "Merma ${merma.id} eliminada")

        println("\nMerma ${merma.id} eliminada.")
        ConsolaUI.mostrarAjusteLote(lote.id, antes, loteController.buscarLotePorId(lote.id).cantidadDisponible)
    }

    private fun elegirCausa(actual: CausaMerma): CausaMerma? {
        val causas = CausaMerma.entries
        println("\nCausa de la merma")
        causas.forEachIndexed { i, causa ->
            val marca = if (causa == actual) "  (actual)" else ""
            println("${i + 1}. ${causa.descripcion}$marca")
        }
        val opcion = Validador.leerOpcion("Seleccione la causa (0 para cancelar): ", 0..causas.size)
        return if (opcion == 0) null else causas[opcion - 1]
    }

    private fun elegirMerma(encabezado: String): Merma? {
        val nombres = productoController.listarProductos().associateBy(Producto::id)
        return elegirDeLista(
            encabezado,
            mermaController.listarMermas(),
            "No hay mermas registradas."
        ) {
            "%-10s %-24s %-10s %8.2f %10s %12s".format(
                it.id, (nombres[it.productoId]?.nombre ?: it.productoId).take(24), it.loteId,
                it.cantidad, ConsolaUI.moneda(it.cantidad * it.costoUnitarioCongelado), ConsolaUI.fecha(it.fecha)
            )
        }
    }

    private fun nombreDe(productoId: String): String =
        productoController.listarProductos().find { it.id == productoId }?.nombre ?: productoId

    companion object {
        private val JAVA = MenuGestionMermas::class.java
    }
}
