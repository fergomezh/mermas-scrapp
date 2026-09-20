package com.ctrlcafe.scrapp.util

import java.time.LocalDate
import java.time.format.DateTimeParseException

object Validador {

    /**
     * Lee una línea de la consola.
     *
     * Si la entrada se cerró (EOF / Ctrl+Z / tubería agotada) lanza
     * [EntradaAgotadaException] en lugar de devolver `null`: tratarlo como
     * "entrada inválida" haría que los bucles de abajo reintentaran sin fin.
     */
    private fun leerLinea(): String = readLine() ?: throw EntradaAgotadaException()

    fun leerTexto(mensaje: String, permitirVacio: Boolean = false): String {
        while (true) {
            print(mensaje)
            val valor = leerLinea().trim()
            if (permitirVacio || valor.isNotEmpty()) return valor
            println("Entrada inválida. No puede quedar vacío.")
        }
    }

    fun leerEntero(mensaje: String, rango: IntRange? = null): Int {
        while (true) {
            print(mensaje)
            val numero = leerLinea().trim().toIntOrNull()

            if (numero != null && (rango == null || numero in rango)) {
                return numero
            }

            val detalle =
                if (rango != null)
                    " entre ${rango.first} y ${rango.last}"
                else
                    ""

            println("Entrada inválida. Ingrese un número entero$detalle.")
        }
    }

    /**
     * Lee un decimal dentro de [minimo]..[maximo] (ambos opcionales e inclusivos).
     *
     * Rechaza NaN e Infinity: `"NaN".toDoubleOrNull()` devuelve `Double.NaN`, y
     * como toda comparación con NaN es `false`, un valor así atravesaría los
     * controles de cantidad y dejaría el stock del lote en NaN.
     */
    fun leerDecimal(mensaje: String, minimo: Double? = null, maximo: Double? = null): Double {
        while (true) {
            print(mensaje)
            val texto = leerLinea().trim().replace(',', '.')
            val numero = texto.toDoubleOrNull()

            if (numero != null && numero.isFinite() &&
                (minimo == null || numero >= minimo) &&
                (maximo == null || numero <= maximo)
            ) {
                return numero
            }

            val detalle = when {
                minimo != null && maximo != null -> " entre %.2f y %.2f".format(minimo, maximo)
                minimo != null -> " mayor o igual a %.2f".format(minimo)
                maximo != null -> " menor o igual a %.2f".format(maximo)
                else -> ""
            }

            println("Entrada inválida. Ingrese un número decimal$detalle.")
        }
    }

    fun leerFecha(mensaje: String): LocalDate {
        while (true) {
            print(mensaje)
            val texto = leerLinea().trim()

            try {
                return LocalDate.parse(texto)
            } catch (e: DateTimeParseException) {
                Logger.error(
                    Validador::class.java,
                    "Fecha inválida ingresada: $texto",
                    e
                )
                println("Fecha inválida. Use el formato AAAA-MM-DD.")
            }
        }
    }

    fun leerOpcion(
        mensaje: String,
        opciones: IntRange
    ): Int = leerEntero(mensaje, opciones)

    /**
     * Valida que la entrada simule una ruta de archivo local (.jpg, .png, etc.)
     * o una URL web (http/https). Permite dejar vacío si es opcional.
     */
    fun leerRutaOUrlImagen(mensaje: String, permitirVacio: Boolean = true): String {
        val extensionesValidas = listOf(".jpg", ".jpeg", ".png", ".webp")
        while (true) {
            val entrada = leerTexto(mensaje, permitirVacio)
            if (entrada.isEmpty()) return entrada

            val esUrl = entrada.startsWith("http://", ignoreCase = true) ||
                    entrada.startsWith("https://", ignoreCase = true)
            val esArchivoImagen = extensionesValidas.any { entrada.endsWith(it, ignoreCase = true) }
            val esRutaLocal = entrada.contains("/") || entrada.contains("\\")

            if (esUrl || esArchivoImagen || esRutaLocal) {
                return entrada
            }

            println("Formato simulado no válido. Ingrese una URL (http://...) o una ruta de imagen válida (.jpg, .png).")
        }
    }
}
