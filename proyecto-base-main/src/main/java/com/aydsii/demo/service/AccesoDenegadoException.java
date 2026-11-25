package com.aydsii.demo.service;

import java.io.Serial;

/**
 * Acceso denegado (403). RN-01 de Consultar estado del pedido: un cliente solo puede consultar sus
 * propios pedidos. Precondición de Actualizar devolución (CU-22): solo Ventas actualiza devoluciones.
 */
public class AccesoDenegadoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }

    public static AccesoDenegadoException pedidoAjeno(Long idUsuario, Long idPedido) {
        return new AccesoDenegadoException("El pedido " + idPedido + " no pertenece al cliente " + idUsuario
                + ". Un cliente solo puede consultar sus propios pedidos.");
    }

    /** CU-22: el empleado no es de Ventas. */
    public static AccesoDenegadoException noEsVentas(Long idEmpleado) {
        return new AccesoDenegadoException("El empleado " + idEmpleado + " no pertenece a Ventas. "
                + "Solo Ventas puede actualizar una devolución.");
    }
}
