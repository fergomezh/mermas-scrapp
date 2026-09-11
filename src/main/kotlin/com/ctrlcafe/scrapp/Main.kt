package com.ctrlcafe.scrapp

import com.ctrlcafe.scrapp.repositorio.*
import com.ctrlcafe.scrapp.util.Logger
import com.ctrlcafe.scrapp.vista.MenuAdministrador
import com.ctrlcafe.scrapp.vista.MenuOperativo
import com.ctrlcafe.scrapp.vista.MenuPrincipal

fun main() {
    Logger.info(MenuPrincipal::class.java, "Inicializando Scrapp en modo consola...")

    val usuarioRepo = UsuarioRepositorio()
    val productoRepo = ProductoRepositorio()
    val loteRepo = LoteRepositorio()
    val ventaRepo = VentaRepositorio()
    val mermaRepo = MermaRepositorio()

    DatosIniciales.cargar(usuarioRepo, productoRepo, loteRepo, ventaRepo)

    val menuOperativo = MenuOperativo(productoRepo, loteRepo, mermaRepo)
    val menuAdministrador = MenuAdministrador(productoRepo, loteRepo, mermaRepo, ventaRepo)
    val menuPrincipal = MenuPrincipal(usuarioRepo, menuOperativo, menuAdministrador)

    menuPrincipal.iniciar()
}
