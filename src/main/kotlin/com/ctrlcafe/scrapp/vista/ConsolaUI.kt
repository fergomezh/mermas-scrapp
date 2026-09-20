package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.modelo.Merma
import com.ctrlcafe.scrapp.modelo.Producto
import com.ctrlcafe.scrapp.servicio.EstadoLote
import com.ctrlcafe.scrapp.servicio.ResultadoRecalculo
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

    val lineaSeparadora = "-".repeat(78)

    fun separador() = println(lineaSeparadora)

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

    /** Recibe los lotes ya evaluados por `MotorSemaforo`, ordenados por urgencia. */
    fun mostrarLotes(estados: List<EstadoLote>) {
        titulo("Monitor de lotes")
        if (estados.isEmpty()) {
            println("No hay lotes registrados.")
            return
        }
        println("%-10s %-28s %10s %12s %-10s %12s".format(
            "LOTE", "PRODUCTO", "DISP.", "CADUCIDAD", "ESTADO", "COSTO"
        ))
        separador()
        estados.forEach { estado ->
            val lote = estado.lote
            val valor = lote.cantidadDisponible * lote.costoUnitario
            println("%-10s %-28s %10.2f %12s %-10s %12s".format(
                lote.id, estado.nombreProducto.take(28), lote.cantidadDisponible,
                fecha(lote.fechaCaducidad), estado.estado.nivelAlerta, moneda(valor)
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

    fun mostrarRecalculo(resultado: ResultadoRecalculo) {
        val merma = resultado.merma
        titulo("Merma registrada")
        println("Merma ${merma.id} | ${resultado.nombreProducto} | Lote ${resultado.loteId}")
        println("Causa: ${merma.causa.descripcion} | Pérdida: ${moneda(resultado.costoPerdida)}")
        println()
        println("%-30s %20s %20s".format("INDICADOR", "ANTES", "DESPUÉS"))
        separador()
        filaRecalculo("Stock del lote",
            "%.2f".format(resultado.cantidadLoteAntes), "%.2f".format(resultado.cantidadLoteDespues))
        filaRecalculo("Estado del lote",
            resultado.estadoLoteAntes.nivelAlerta, resultado.estadoLoteDespues.nivelAlerta)
        filaRecalculo("Pérdida del día",
            moneda(resultado.perdidaDiaAntes), moneda(resultado.perdidaDiaDespues))
        filaRecalculo("Tasa de merma (14 días)",
            "%.2f%%".format(resultado.tasaMermaAntes * 100), "%.2f%%".format(resultado.tasaMermaDespues * 100))
        filaRecalculo("Producción sugerida mañana",
            "%.2f".format(resultado.sugeridaAntes), "%.2f".format(resultado.sugeridaDespues))

        if (resultado.loteQuedoVacio) println("\nAviso: el lote ${resultado.loteId} quedó sin existencias.")
        // cambioEstadoLote compara el nivel de alerta, no el objeto: Critico/ProximoAVencer
        // llevan las horas restantes dentro y cambian con solo pasar una hora.
        if (resultado.cambioEstadoLote) {
            println("Aviso: el lote pasó de ${resultado.estadoLoteAntes.nivelAlerta} a ${resultado.estadoLoteDespues.nivelAlerta}.")
        }
    }

    private fun filaRecalculo(indicador: String, antes: String, despues: String) =
        println("%-30s %20s %20s".format(indicador, antes, despues))

    fun barra(nombre: String, valor: Double, maximo: Double, ancho: Int = 35): String {
        val n = if (maximo <= 0) 0 else ((valor / maximo) * ancho).toInt().coerceIn(0, ancho)
        return "%-28s |%s %s".format(nombre.take(28), "#".repeat(n), moneda(valor))
    }

    // ------------------------------------------------------ Vistas de detalle

    /** Ficha de un producto, para el submenú de gestión. */
    fun mostrarProductoDetalle(producto: Producto) {
        println("\nProducto ${producto.id}")
        separador()
        println("Nombre           : ${producto.nombre}")
        println("Categoría        : ${producto.categoria}")
        println("Costo unitario   : ${moneda(producto.costoUnitario)}")
        println("Precio de venta  : ${moneda(producto.precioVenta)}")
        println("Unidad de medida : ${producto.unidadMedida}")
        val margen = producto.precioVenta - producto.costoUnitario
        println("Margen por unidad: ${moneda(margen)}")
    }

    /** Ficha de un lote junto al historial de mermas que se le han cargado. */
    fun mostrarLoteDetalle(estado: EstadoLote, mermas: List<Merma>) {
        val lote = estado.lote
        println("\nLote ${lote.id}")
        separador()
        println("Producto         : ${estado.nombreProducto} (${lote.productoId})")
        println("Cantidad inicial : %.2f".format(lote.cantidadInicial))
        println("Disponible       : %.2f".format(lote.cantidadDisponible))
        println("Costo unitario   : ${moneda(lote.costoUnitario)}")
        println("Valor en stock   : ${moneda(lote.cantidadDisponible * lote.costoUnitario)}")
        println("Ingreso          : ${fecha(lote.fechaIngreso)} (vida útil ${lote.vidaUtilDias} días)")
        println("Caducidad        : ${fecha(lote.fechaCaducidad)}")
        println("Estado           : ${estado.estado.nivelAlerta} - ${estado.estado.descripcion}")

        println("\nMermas cargadas a este lote:")
        if (mermas.isEmpty()) {
            println("Ninguna.")
            return
        }
        println("%-10s %10s %12s %12s %-26s".format("ID", "CANT.", "PÉRDIDA", "FECHA", "CAUSA"))
        separador()
        mermas.forEach {
            println("%-10s %10.2f %12s %12s %-26s".format(
                it.id, it.cantidad, moneda(it.cantidad * it.costoUnitarioCongelado),
                fecha(it.fecha), it.causa.descripcion.take(26)
            ))
        }
        println("%-10s %10.2f %12s".format(
            "TOTAL", mermas.sumOf { it.cantidad },
            moneda(mermas.sumOf { it.cantidad * it.costoUnitarioCongelado })
        ))
    }

    /** Ficha de una merma con los campos que la tabla resumida no alcanza a mostrar. */
    fun mostrarMermaDetalle(merma: Merma, nombreProducto: String) {
        println("\nMerma ${merma.id}")
        separador()
        println("Producto         : $nombreProducto (${merma.productoId})")
        println("Lote             : ${merma.loteId}")
        println("Cantidad         : %.2f".format(merma.cantidad))
        println("Costo congelado  : ${moneda(merma.costoUnitarioCongelado)}")
        println("Pérdida          : ${moneda(merma.cantidad * merma.costoUnitarioCongelado)}")
        println("Fecha            : ${fecha(merma.fecha)}")
        println("Causa            : ${merma.causa.descripcion}")
        println("Evidencia        : ${merma.rutaEvidencia}")
        println("Registrada por   : ${merma.usuarioId}")
    }

    /** Resume en una línea el efecto de una edición o borrado sobre el stock del lote. */
    fun mostrarAjusteLote(loteId: String, antes: Double, despues: Double) {
        println("Lote %s: %.2f -> %.2f unidades disponibles.".format(loteId, antes, despues))
    }
}
