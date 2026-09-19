package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.controlador.AuthController
import com.ctrlcafe.scrapp.controlador.LoteController
import com.ctrlcafe.scrapp.controlador.MermaController
import com.ctrlcafe.scrapp.controlador.ProductoController
import com.ctrlcafe.scrapp.modelo.Accion
import com.ctrlcafe.scrapp.modelo.Usuario
import com.ctrlcafe.scrapp.servicio.MotorFinanciero
import com.ctrlcafe.scrapp.servicio.MotorProyeccion
import com.ctrlcafe.scrapp.servicio.MotorSemaforo
import com.ctrlcafe.scrapp.util.Logger
import com.ctrlcafe.scrapp.util.ScrappException
import com.ctrlcafe.scrapp.util.Validador
import java.io.File
import java.time.LocalDate

class MenuAdministrador(
    private val auth: AuthController,
    private val productoController: ProductoController,
    private val loteController: LoteController,
    private val mermaController: MermaController,
    private val semaforo: MotorSemaforo,
    private val financiero: MotorFinanciero,
    private val proyeccion: MotorProyeccion,
    private val registroMerma: FlujoRegistroMerma,
    private val gestionProductos: MenuGestionProductos,
    private val gestionLotes: MenuGestionLotes,
    private val gestionMermas: MenuGestionMermas
) {
    fun mostrar(usuario: Usuario) {
        while (true) {
            ConsolaUI.titulo("Menú Administrador")
            println("Usuario: ${usuario.nombreCompleto}")
            println("1. Ver productos")
            println("2. Monitor de lotes")
            println("3. Ver mermas")
            println("4. Resumen / reporte")
            println("5. Registrar merma")
            println("6. Gestionar productos")
            println("7. Gestionar lotes")
            println("8. Gestionar mermas")
            println("0. Cerrar sesión")

            val opcion = Validador.leerOpcion("Seleccione una opción: ", 0..8)
            when (opcion) {
                1 -> protegido { ConsolaUI.mostrarProductos(productoController.listarProductos()) }
                2 -> protegido { ConsolaUI.mostrarLotes(semaforo.recalcularTodos(incluirAgotados = true)) }
                3 -> protegido {
                    ConsolaUI.mostrarMermas(mermaController.listarMermas(), productoController.listarProductos())
                }
                4 -> protegido {
                    auth.verificarPermiso(Accion.VER_REPORTES_FINANCIEROS)
                    mostrarReporte()
                }
                5 -> protegido { registroMerma.ejecutar() }
                6 -> gestionProductos.mostrar()
                7 -> gestionLotes.mostrar()
                8 -> gestionMermas.mostrar()
                0 -> return
            }
            if (opcion in 1..5) ConsolaUI.pausa()
        }
    }

    /** Traduce un permiso denegado o una sesión caída en un mensaje, sin tumbar el menú. */
    private fun protegido(accion: () -> Unit) {
        try {
            accion()
        } catch (e: ScrappException) {
            Logger.error(MenuAdministrador::class.java, "Operación rechazada en el menú administrador", e)
            println("\nOperación no disponible. ${e.message}")
        }
    }

    /** Resumen del periodo con los motores de cálculo; se muestra en consola y se exporta igual a un .txt. */
    private fun mostrarReporte() {
        ConsolaUI.titulo("Resumen de control de mermas")
        val hoy = LocalDate.now()
        val reporte = construirReporte(hoy.minusDays(DIAS_REPORTE - 1), hoy)
        print(reporte)
        exportarReporte(reporte, hoy)
    }

    private fun construirReporte(desde: LocalDate, hasta: LocalDate): String = buildString {
        val resumen = financiero.resumen(desde, hasta)
        val semaforoTexto = semaforo.conteoPorEstado().entries.joinToString(" | ") { (nivel, total) -> "$nivel=$total" }

        appendLine("Periodo             : ${ConsolaUI.fecha(desde)} - ${ConsolaUI.fecha(hasta)} ($DIAS_REPORTE días)")
        appendLine("Productos activos   : ${productoController.listarProductos().size}")
        appendLine("Lotes registrados   : ${loteController.listarLotes().size}")
        appendLine("Semáforo            : $semaforoTexto (lotes con existencias)")
        appendLine("Pérdida del periodo : ${ConsolaUI.moneda(resumen.costoMermas)} (%.2f unidades)".format(resumen.unidadesPerdidas))
        appendLine("Costo de producción : ${ConsolaUI.moneda(resumen.costoProduccionTotal)}")
        appendLine("Índice de merma     : %.2f%%".format(resumen.indiceMerma))
        resumen.advertencia?.let { appendLine("Aviso: $it") }

        appendLine("\nTop 5 productos críticos:")
        if (resumen.criticos.isEmpty()) appendLine("Sin mermas registradas en el periodo.")
        val maximo = resumen.criticos.maxOfOrNull { it.costoPerdida } ?: 0.0
        resumen.criticos.forEachIndexed { i, critico ->
            appendLine("${i + 1}. ${ConsolaUI.barra(critico.nombreProducto, critico.costoPerdida, maximo)}" +
                " (%.1f%% del total)".format(critico.porcentajeDelTotal))
        }

        val manana = hasta.plusDays(1)
        val proyecciones = proyeccion.proyectarTodos(manana)
        appendLine("\nProducción sugerida para ${ConsolaUI.fecha(manana)}:")
        appendLine("%-28s %14s %12s %12s %10s".format("PRODUCTO", "DEMANDA BASE", "TASA MERMA", "SUGERIDA", "HISTORIAL"))
        appendLine(ConsolaUI.lineaSeparadora)
        proyecciones.forEach { p ->
            // tieneHistorialCompleto/semanasConDatos explican de cuántas semanas salió la demanda base.
            val historial = if (p.tieneHistorialCompleto) "completo"
                else "${p.semanasConDatos}/${p.ventasEquivalentes.size}"
            appendLine("%-28s %14.2f %11.2f%% %12.2f %10s".format(
                p.nombreProducto.take(28), p.demandaBase, p.tasaMerma * 100, p.cantidadSugerida, historial
            ))
        }
        val conAdvertencia = proyecciones.filter { it.advertencia != null }
        if (conAdvertencia.isNotEmpty()) {
            appendLine("\nNotas de la proyección:")
            conAdvertencia.forEach { appendLine("- ${it.nombreProducto}: ${it.advertencia}") }
        }
    }

    private fun exportarReporte(reporte: String, fecha: LocalDate) {
        try {
            val dir = File("reportes")
            if (!dir.exists()) dir.mkdirs()
            val archivo = File(dir, "resumen_$fecha.txt")
            archivo.writeText("SCRAPP - RESUMEN DE CONTROL DE MERMAS\nGenerado: ${ConsolaUI.fecha(fecha)}\n\n$reporte")
            println("\nReporte exportado en: ${archivo.path}")
        } catch (e: Exception) {
            Logger.error(MenuAdministrador::class.java, "No se pudo exportar el reporte", e)
            println("No fue posible exportar el reporte. Revise logs/errores.log.")
        }
    }

    companion object {
        /** Ventana del resumen: los últimos 30 días, incluido hoy. */
        private const val DIAS_REPORTE = 30L
    }
}
