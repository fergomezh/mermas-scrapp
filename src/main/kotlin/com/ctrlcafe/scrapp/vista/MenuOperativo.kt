package com.ctrlcafe.scrapp.vista

import com.ctrlcafe.scrapp.controlador.MermaController
import com.ctrlcafe.scrapp.controlador.ProductoController
import com.ctrlcafe.scrapp.modelo.Usuario
import com.ctrlcafe.scrapp.servicio.MotorSemaforo
import com.ctrlcafe.scrapp.util.ScrappException
import com.ctrlcafe.scrapp.util.Logger
import com.ctrlcafe.scrapp.util.Validador

class MenuOperativo(
    private val productoController: ProductoController,
    private val mermaController: MermaController,
    private val semaforo: MotorSemaforo,
    private val registroMerma: FlujoRegistroMerma
) {
    fun mostrar(usuario: Usuario) {
        while (true) {
            ConsolaUI.titulo("Menú Operativo")
            println("Usuario: ${usuario.nombreCompleto}")
            println("1. Ver productos")
            println("2. Consultar stock y lotes")
            println("3. Ver mermas registradas")
            println("4. Registrar merma")
            println("0. Cerrar sesión")

            when (Validador.leerOpcion("Seleccione una opción: ", 0..4)) {
                // El control de permisos ya no se duplica aquí: cada controlador
                // verifica la acción contra el usuario en sesión y lanza si no procede.
                1 -> protegido { ConsolaUI.mostrarProductos(productoController.listarProductos()) }
                2 -> protegido { ConsolaUI.mostrarLotes(semaforo.recalcularTodos(incluirAgotados = true)) }
                3 -> protegido {
                    ConsolaUI.mostrarMermas(mermaController.listarMermas(), productoController.listarProductos())
                }
                4 -> protegido { registroMerma.ejecutar() }
                0 -> return
            }
            ConsolaUI.pausa()
        }
    }

    /** Traduce un permiso denegado o una sesión caída en un mensaje, sin tumbar el menú. */
    private fun protegido(accion: () -> Unit) {
        try {
            accion()
        } catch (e: ScrappException) {
            Logger.error(MenuOperativo::class.java, "Operación rechazada en el menú operativo", e)
            println("\nOperación no disponible. ${e.message}")
        }
    }
}
