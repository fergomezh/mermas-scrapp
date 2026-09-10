package com.ctrlcafe.scrapp.repositorio

import com.ctrlcafe.scrapp.modelo.*
import java.time.LocalDate

object DatosIniciales {

    fun cargar(
        usuarioRepo: UsuarioRepositorio,
        productoRepo: ProductoRepositorio,
        loteRepo: LoteRepositorio,
        ventaRepo: VentaRepositorio
    ) {
        val hoy = LocalDate.now()

        usuarioRepo.crear(Administrador("USR-01", "admin", "admin123", "Carlos Gómez (Gerente)"))
        usuarioRepo.crear(Operativo("USR-02", "barista", "barista123", "Sofía Ramos (Barista)"))

        val productos = listOf(
            Producto("PROD-01", "Café Grano Especial 1kg", "Granos", 8.50, 15.00, "Bolsa"),
            Producto("PROD-02", "Leche Entera 1L", "Lácteos", 1.10, 1.85, "Litro"),
            Producto("PROD-03", "Croissant de Mantequilla", "Panadería", 0.65, 1.75, "Unidad"),
            Producto("PROD-04", "Cheesecake de Frutos Rojos", "Repostería", 1.80, 4.00, "Porción"),
            Producto("PROD-05", "Jarabe de Vainilla 750ml", "Insumos", 4.20, 7.50, "Botella"),
            Producto("PROD-06", "Pan Baguette", "Panadería", 0.40, 1.25, "Unidad"),
            Producto("PROD-07", "Muffins de Arándano", "Repostería", 0.55, 1.50, "Unidad")
        )
        productos.forEach { productoRepo.crear(it) }

        val lotes = listOf(
            Lote("LOT-001", "PROD-01", 30.0, 24.0, 8.50, hoy.minusDays(10), 55, hoy.plusDays(45)),
            Lote("LOT-002", "PROD-02", 50.0, 18.0, 1.10, hoy.minusDays(3), 18, hoy.plusDays(15)),
            Lote("LOT-003", "PROD-02", 40.0, 12.0, 1.10, hoy.minusDays(5), 9, hoy.plusDays(4)),
            Lote("LOT-004", "PROD-03", 25.0, 10.0, 0.65, hoy.minusDays(1), 2, hoy.plusDays(1)),
            Lote("LOT-005", "PROD-04", 15.0, 6.0, 1.80, hoy.minusDays(2), 4, hoy.plusDays(2)),
            Lote("LOT-006", "PROD-04", 10.0, 4.0, 1.80, hoy.minusDays(6), 5, hoy.minusDays(1)),
            Lote("LOT-007", "PROD-05", 20.0, 20.0, 4.20, hoy.minusDays(15), 105, hoy.plusDays(90)),
            Lote("LOT-008", "PROD-06", 30.0, 8.0, 0.40, hoy.minusDays(2), 1, hoy.minusDays(1)),
            Lote("LOT-009", "PROD-07", 20.0, 14.0, 0.55, hoy.minusDays(1), 6, hoy.plusDays(5))
        )
        lotes.forEach { loteRepo.crear(it) }

        val ventas = listOf(
            RegistroVenta("VNT-001", "PROD-01", 12.0, hoy.minusWeeks(4)),
            RegistroVenta("VNT-002", "PROD-01", 15.0, hoy.minusWeeks(3)),
            RegistroVenta("VNT-003", "PROD-01", 14.0, hoy.minusWeeks(2)),
            RegistroVenta("VNT-004", "PROD-01", 16.0, hoy.minusWeeks(1)),

            RegistroVenta("VNT-005", "PROD-02", 40.0, hoy.minusWeeks(4)),
            RegistroVenta("VNT-006", "PROD-02", 42.0, hoy.minusWeeks(3)),
            RegistroVenta("VNT-007", "PROD-02", 38.0, hoy.minusWeeks(2)),
            RegistroVenta("VNT-008", "PROD-02", 45.0, hoy.minusWeeks(1)),

            RegistroVenta("VNT-009", "PROD-03", 50.0, hoy.minusWeeks(3)),
            RegistroVenta("VNT-010", "PROD-03", 55.0, hoy.minusWeeks(2)),
            RegistroVenta("VNT-011", "PROD-03", 48.0, hoy.minusWeeks(1)),

            RegistroVenta("VNT-012", "PROD-04", 18.0, hoy.minusWeeks(3)),
            RegistroVenta("VNT-013", "PROD-04", 22.0, hoy.minusWeeks(2)),
            RegistroVenta("VNT-014", "PROD-04", 20.0, hoy.minusWeeks(1))
        )
        ventas.forEach { ventaRepo.crear(it) }
    }
}