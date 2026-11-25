package com.aydsii.demo.service;

import java.io.Serial;

/**
 * El recurso pedido no existe (404): el pedido (CU-09 y CU-13), el producto dentro del pedido
 * (CU-13), la solicitud de devolución o el empleado (CU-22).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Título corto que va en el {@code ErrorDTO}. */
    private final String titulo;

    private RecursoNoEncontradoException(String titulo, String mensaje) {
        super(mensaje);
        this.titulo = titulo;
    }

    public static RecursoNoEncontradoException pedido(Long idPedido) {
        return new RecursoNoEncontradoException("Pedido no encontrado", "No existe el pedido " + idPedido + ".");
    }

    /** CU-13: el producto indicado no forma parte del pedido. */
    public static RecursoNoEncontradoException productoEnPedido(Long idPedido, Long idProducto) {
        return new RecursoNoEncontradoException("Producto no encontrado",
                "El producto " + idProducto + " no forma parte del pedido " + idPedido + ".");
    }

    /** FE-01 de CU-22: el número de devolución no corresponde a una solicitud registrada. */
    public static RecursoNoEncontradoException solicitud(Long idSolicitud) {
        return new RecursoNoEncontradoException("Solicitud de devolución no encontrada",
                "El número de devolución " + idSolicitud + " no es válido: no corresponde a una solicitud registrada.");
    }

    /** CU-22: el empleado que actualiza la devolución no existe. */
    public static RecursoNoEncontradoException empleado(Long idEmpleado) {
        return new RecursoNoEncontradoException("Empleado no encontrado", "No existe el empleado " + idEmpleado + ".");
    }

    public String getTitulo() {
        return titulo;
    }
}
