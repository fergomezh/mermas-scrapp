package com.ctrlcafe.scrapp

import com.ctrlcafe.scrapp.controlador.*
import com.ctrlcafe.scrapp.repositorio.*
import com.ctrlcafe.scrapp.servicio.*
import com.ctrlcafe.scrapp.util.EntradaAgotadaException
import com.ctrlcafe.scrapp.util.Logger
import com.ctrlcafe.scrapp.vista.FlujoRegistroMerma
import com.ctrlcafe.scrapp.vista.MenuAdministrador
import com.ctrlcafe.scrapp.vista.MenuGestionLotes
import com.ctrlcafe.scrapp.vista.MenuGestionMermas
import com.ctrlcafe.scrapp.vista.MenuGestionProductos
import com.ctrlcafe.scrapp.vista.MenuOperativo
import com.ctrlcafe.scrapp.vista.MenuPrincipal

fun main() {
    Logger.info(MenuPrincipal::class.java, "Inicializando Scrapp en modo consola...")

    // 1. Repositorios
    val usuarioRepo = UsuarioRepositorio()
    val productoRepo = ProductoRepositorio()
    val loteRepo = LoteRepositorio()
    val ventaRepo = VentaRepositorio()
    val mermaRepo = MermaRepositorio()

    // Carga de datos semilla iniciales
    DatosIniciales.cargar(usuarioRepo, productoRepo, loteRepo, ventaRepo)

    // 2. Instanciación del sistema de autenticación y motores de cálculo
    val auth = AuthController(usuarioRepo)
    val config = ConfiguracionMotores()
    val semaforo = MotorSemaforo(loteRepo, productoRepo, config)
    val financiero = MotorFinanciero(mermaRepo, loteRepo, productoRepo)
    val proyeccion = MotorProyeccion(ventaRepo, mermaRepo, loteRepo, productoRepo, config)

    // 3. Controladores y Orquestador
    // La vista NO habla con los repositorios: toda lectura pasa por un controlador,
    // que es quien verifica el permiso del usuario en sesión.
    val productoController = ProductoController(productoRepo, loteRepo, mermaRepo, auth)
    val loteController = LoteController(loteRepo, productoRepo, auth, semaforo)
    val mermaController = MermaController(mermaRepo, loteRepo, productoRepo, auth)
    val orquestador = OrquestadorMerma(loteRepo, mermaRepo, productoRepo, semaforo, financiero, proyeccion)

    // 4. Vistas (Pasando las dependencias requeridas a los menús)
    val registroMerma = FlujoRegistroMerma(
        mermaController, productoController, orquestador, semaforo
    )
    val menuOperativo = MenuOperativo(productoController, mermaController, semaforo, registroMerma)

    // Submenus de administracion: cada uno solo conoce los controladores que usa.
    val gestionProductos = MenuGestionProductos(productoController, loteController, semaforo)
    val gestionLotes = MenuGestionLotes(loteController, productoController, mermaRepo, semaforo)
    val gestionMermas = MenuGestionMermas(mermaController, productoController, loteController)

    val menuAdministrador = MenuAdministrador(
        auth, productoController, loteController, mermaController,
        semaforo, financiero, proyeccion, registroMerma,
        gestionProductos, gestionLotes, gestionMermas
    )
    val menuPrincipal = MenuPrincipal(auth, menuOperativo, menuAdministrador)

    // Arrancamos la aplicación
    try {
        menuPrincipal.iniciar()
    } catch (e: EntradaAgotadaException) {
        // Ctrl+Z, EOF o una tubería agotada: salimos ordenadamente en vez de
        // quedarnos leyendo null para siempre.
        Logger.info(MenuPrincipal::class.java, "Entrada estándar cerrada; finalizando.")
        println("\n${e.message}")
    }
}
