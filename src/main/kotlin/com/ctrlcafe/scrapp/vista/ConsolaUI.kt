package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.modelo.EstadoCaducidad
import com.ctrlcafe.scrapp.modelo.Lote
import com.ctrlcafe.scrapp.modelo.Merma
import com.ctrlcafe.scrapp.modelo.Producto
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ConsolaUI {
    private val moneda = NumberFormat.getCurrencyInstance(Locale.US)
    private val fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun limpiar() {
        repeat(2) { println() }
    }

    fun titulo(texto: String) {
        println()
        println("=".repeat(78))
        println(texto.uppercase().padStart((78 + texto.length) / 2).take(78))
        println("=".repeat(78))
    }

    fun separador() = println("-".repeat(78))

    fun pausa() {
        print("\nPresione ENTER para continuar...")
        readLine()
    }

    fun moneda(valor: Double): String = moneda.format(valor)
    fun fecha(valor: LocalDate): String = valor.format(fecha)

    fun mostrarProductos(productos: List<Producto>) {
        titulo("Productos")
        if (productos.isEmpty()) {
            println("No hay productos registrados.")
            return
        }
        println("%-10s %-30s %-18s %12s".format("ID", "NOMBRE", "CATEGORÍA", "COSTO"))
        separador()
        productos.forEach {
            println("%-10s %-30s %-18s %12s".format(
                it.id, it.nombre.take(30), it.categoria.take(18), moneda(it.costoUnitario)
            ))
        }
    }

    fun estado(lote: Lote): String {
        return when (lote.estado) {
            is EstadoCaducidad.Vigente -> "VERDE"
            is EstadoCaducidad.ProximoAVencer -> "AMARILLO"
            is EstadoCaducidad.Critico -> "ROJO"
            is EstadoCaducidad.Vencido -> "NEGRO"
        }
    }

    fun actualizarEstado(lote: Lote): EstadoCaducidad {
        val horas = java.time.Duration.between(
            java.time.LocalDateTime.now(),
            lote.fechaCaducidad.atStartOfDay()
        ).toHours()
        return when {
            horas < 0 -> EstadoCaducidad.Vencido
            horas <= 24 -> EstadoCaducidad.Critico(horas)
            horas <= 72 -> EstadoCaducidad.ProximoAVencer(horas)
            else -> EstadoCaducidad.Vigente
        }
    }

    fun mostrarLotes(lotes: List<Lote>, productos: List<Producto>) {
        titulo("Monitor de lotes")
        if (lotes.isEmpty()) {
            println("No hay lotes registrados.")
            return
        }
        val mapa = productos.associateBy { it.id }
        println("%-10s %-28s %10s %12s %-10s %12s".format(
            "LOTE", "PRODUCTO", "DISP.", "CADUCIDAD", "ESTADO", "COSTO"
        ))
        separador()
        lotes.sortedBy { it.fechaCaducidad }.forEach { lote ->
            lote.estado = actualizarEstado(lote)
            val producto = mapa[lote.productoId]?.nombre ?: lote.productoId
            val valor = lote.cantidadDisponible * lote.costoUnitario
            println("%-10s %-28s %10.2f %12s %-10s %12s".format(
                lote.id, producto.take(28), lote.cantidadDisponible,
                fecha(lote.fechaCaducidad), estado(lote), moneda(valor)
            ))
        }
    }

    fun mostrarMermas(mermas: List<Merma>, productos: List<Producto>) {
        titulo("Registro de mermas")
        if (mermas.isEmpty()) {
            println("No hay mermas registradas.")
            return
        }
        val mapa = productos.associateBy { it.id }
        println("%-10s %-26s %10s %15s %-24s".format(
            "ID", "PRODUCTO", "CANT.", "PÉRDIDA", "CAUSA"
        ))
        separador()
        mermas.forEach {
            println("%-10s %-26s %10.2f %15s %-24s".format(
                it.id, (mapa[it.productoId]?.nombre ?: it.productoId).take(26),
                it.cantidad, moneda(it.cantidad * it.costoUnitarioCongelado),
                it.causa.descripcion.take(24)
            ))
        }
    }

    fun barra(nombre: String, valor: Double, maximo: Double, ancho: Int = 35) {
        val n = if (maximo <= 0) 0 else ((valor / maximo) * ancho).toInt().coerceIn(0, ancho)
        println("%-28s |%s %.2f".format(nombre.take(28), "#".repeat(n), valor))
    }
}
