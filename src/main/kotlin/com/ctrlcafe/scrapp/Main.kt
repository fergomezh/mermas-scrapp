package com.ctrlcafe.scrapp

import com.ctrlcafe.scrapp.repositorio.*
import com.ctrlcafe.scrapp.util.Logger

fun main() {
    Logger.info(Unit::class.java, "Inicializando Scrapp en modo consola...")

    val usuarioRepo = UsuarioRepositorio()
    val productoRepo = ProductoRepositorio()
    val loteRepo = LoteRepositorio()
    val ventaRepo = VentaRepositorio()

    DatosIniciales.cargar(usuarioRepo, productoRepo, loteRepo, ventaRepo)

    println("Scrapp CLI inicializado exitosamente.")
    println("Usuarios cargados: ${usuarioRepo.listar().size}")
    println("Productos cargados: ${productoRepo.listar().size}")
    println("Lotes registrados: ${loteRepo.listar().size}")
    println("Ventas historicas: ${ventaRepo.listar().size}")

}