package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.util.Logger
import com.ctrlcafe.scrapp.util.ScrappException
import com.ctrlcafe.scrapp.util.Validador

/**
 * Utilidades compartidas por los menús de consola.
 *
 * Están fuera de `ConsolaUI` a propósito: `ConsolaUI` solo pinta, esto además
 * lee del teclado y maneja errores de negocio.
 */

/**
 * Ejecuta [accion] y traduce un error de negocio en un mensaje legible.
 * Un fallo de programación (NPE, índice fuera de rango) se sigue propagando:
 * esos no se deben tapar.
 */
internal fun protegido(
    origen: Class<*>,
    contexto: String = "Operación no disponible",
    accion: () -> Unit
) {
    try {
        accion()
    } catch (e: Exception) {
        if (e !is ScrappException && e !is IllegalArgumentException && e !is IllegalStateException) throw e
        Logger.error(origen, contexto, e)
        println("\n$contexto. ${e.message}")
    }
}

/**
 * Muestra [items] numerados y devuelve el elegido, o `null` si el usuario
 * cancela con 0 o si la lista viene vacía.
 */
internal fun <T> elegirDeLista(
    encabezado: String,
    items: List<T>,
    vacio: String,
    etiqueta: (T) -> String
): T? {
    if (items.isEmpty()) {
        println("\n$vacio")
        return null
    }
    println("\n$encabezado")
    ConsolaUI.separador()
    items.forEachIndexed { i, item -> println("%-4d %s".format(i + 1, etiqueta(item))) }
    val opcion = Validador.leerOpcion("\nSeleccione una opción (0 para cancelar): ", 0..items.size)
    return if (opcion == 0) null else items[opcion - 1]
}

/** Confirmación explícita antes de una operación que escribe. */
internal fun confirmar(mensaje: String = "\n1. Confirmar  |  0. Cancelar: "): Boolean =
    Validador.leerOpcion(mensaje, 0..1) == 1

/** Mensaje único para las cancelaciones, así todos los submenús se ven igual. */
internal fun cancelado() = println("\nOperación cancelada.")
/**
 * Siguiente ID correlativo a partir del mayor existente con ese prefijo.
 * Mismo criterio que usa el registro de mermas para los IDs `MER-###`.
 */
internal fun siguienteId(existentes: List<String>, prefijo: String, digitos: Int): String {
    val ultimo = existentes
        .filter { it.startsWith(prefijo) }
        .mapNotNull { it.removePrefix(prefijo).toIntOrNull() }
        .maxOrNull() ?: 0
    return prefijo + (ultimo + 1).toString().padStart(digitos, '0')
}
