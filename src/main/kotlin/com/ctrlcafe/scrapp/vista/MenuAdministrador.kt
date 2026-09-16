package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.modelo.Accion
import com.ctrlcafe.scrapp.modelo.Usuario
import com.ctrlcafe.scrapp.repositorio.LoteRepositorio
import com.ctrlcafe.scrapp.repositorio.MermaRepositorio
import com.ctrlcafe.scrapp.repositorio.ProductoRepositorio
import com.ctrlcafe.scrapp.repositorio.VentaRepositorio
import com.ctrlcafe.scrapp.util.Validador
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MenuAdministrador(
    private val productoRepo: ProductoRepositorio,
    private val loteRepo: LoteRepositorio,
    private val mermaRepo: MermaRepositorio,
    private val ventaRepo: VentaRepositorio,
    private val registroMerma: FlujoRegistroMerma
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
            println("0. Cerrar sesión")

            when (Validador.leerOpcion("Seleccione una opción: ", 0..5)) {
                1 -> ConsolaUI.mostrarProductos(productoRepo.listar())
                2 -> ConsolaUI.mostrarLotes(loteRepo.listar(), productoRepo.listar())
                3 -> ConsolaUI.mostrarMermas(mermaRepo.listar(), productoRepo.listar())
                4 -> mostrarReporte()
                5 -> registroMerma.ejecutar()
                0 -> return
            }
            if (Validador.leerOpcion("0. Volver  |  1. Continuar: ", 0..1) == 0) continue
        }
    }

    private fun mostrarReporte() {
        ConsolaUI.titulo("Resumen de control de mermas")
        val productos = productoRepo.listar()
        val lotes = loteRepo.listar()
        val mermas = mermaRepo.listar()

        lotes.forEach { it.estado = ConsolaUI.actualizarEstado(it) }

        val verdes = lotes.count { ConsolaUI.estado(it) == "VERDE" }
        val amarillos = lotes.count { ConsolaUI.estado(it) == "AMARILLO" }
        val rojos = lotes.count { ConsolaUI.estado(it) == "ROJO" }
        val negros = lotes.count { ConsolaUI.estado(it) == "NEGRO" }

        val perdida = mermas.sumOf { it.cantidad * it.costoUnitarioCongelado }
        val valorInventario = lotes.sumOf { it.cantidadDisponible * it.costoUnitario }
        val indice = if (valorInventario > 0) perdida / valorInventario * 100 else 0.0

        println("Productos activos : ${productos.size}")
        println("Lotes registrados : ${lotes.size}")
        println("Semáforo           : VERDE=$verdes | AMARILLO=$amarillos | ROJO=$rojos | NEGRO=$negros")
        println("Pérdida acumulada  : ${ConsolaUI.moneda(perdida)}")
        println("Índice de merma    : %.2f%%".format(indice))

        val top = mermas.groupBy { it.productoId }
            .mapValues { (_, lista) -> lista.sumOf { it.cantidad * it.costoUnitarioCongelado } }
            .entries.sortedByDescending { it.value }.take(5)
        println("\nTop 5 productos críticos:")
        val nombres = productos.associate { it.id to it.nombre }
        val max = top.maxOfOrNull { it.value } ?: 0.0
        if (top.isEmpty()) println("Sin mermas registradas.")
        top.forEachIndexed { i, entry ->
            print("${i + 1}. ")
            ConsolaUI.barra(nombres[entry.key] ?: entry.key, entry.value, max)
        }

        val promedioDiario = ventasPromedioDiario()
        val proyeccion = perdida + promedioDiario
        println("\nProyección simple siguiente día: ${ConsolaUI.moneda(proyeccion)}")
        println("(Base provisional: pérdida acumulada + promedio diario de ventas históricas.)")

        exportarReporte(productos.size, lotes.size, verdes, amarillos, rojos, negros, perdida, indice, top, nombres, proyeccion)
    }

    private fun ventasPromedioDiario(): Double {
        val hoy = LocalDate.now()
        val ventas = ventaRepo.listar().filter { ChronoUnit.DAYS.between(it.fecha, hoy) in 1..28 }
        return if (ventas.isEmpty()) 0.0 else ventas.sumOf { it.cantidad } / 28.0
    }

    private fun exportarReporte(
        productos: Int, lotes: Int, verdes: Int, amarillos: Int, rojos: Int, negros: Int,
        perdida: Double, indice: Double, top: List<Map.Entry<String, Double>>,
        nombres: Map<String, String>, proyeccion: Double
    ) {
        try {
            val dir = java.io.File("reportes")
            if (!dir.exists()) dir.mkdirs()
            val archivo = java.io.File(dir, "resumen_${LocalDate.now()}.txt")
            archivo.writeText(buildString {
                appendLine("SCRAPP - RESUMEN DE CONTROL DE MERMAS")
                appendLine("Fecha: ${LocalDate.now()}")
                appendLine("Productos activos: $productos")
                appendLine("Lotes registrados: $lotes")
                appendLine("Semáforo: VERDE=$verdes | AMARILLO=$amarillos | ROJO=$rojos | NEGRO=$negros")
                appendLine("Pérdida acumulada: ${ConsolaUI.moneda(perdida)}")
                appendLine("Índice de merma: %.2f%%".format(indice))
                appendLine("\nTOP 5 PRODUCTOS CRÍTICOS")
                top.forEachIndexed { i, e -> appendLine("${i + 1}. ${nombres[e.key] ?: e.key}: ${ConsolaUI.moneda(e.value)}") }
                appendLine("\nProyección siguiente día: ${ConsolaUI.moneda(proyeccion)}")
            })
            println("\nReporte exportado en: ${archivo.path}")
        } catch (e: Exception) {
            com.ctrlcafe.scrapp.util.Logger.error(MenuAdministrador::class.java, "No se pudo exportar el reporte", e)
            println("No fue posible exportar el reporte. Revise logs/errores.log.")
        }
    }
}
