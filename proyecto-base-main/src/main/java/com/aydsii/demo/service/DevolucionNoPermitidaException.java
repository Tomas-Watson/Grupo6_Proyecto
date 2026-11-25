package com.aydsii.demo.service;

import com.aydsii.demo.domain.EstadoDevolucion;
import com.aydsii.demo.domain.EstadoPedido;

import java.io.Serial;

/**
 * La devolución no se puede pedir o no se puede actualizar por una regla de negocio (409). No se
 * registra nada: aplica la postcondición de fallo del caso de uso.
 */
public class DevolucionNoPermitidaException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Título corto que va en el {@code ErrorDTO}. */
    private final String titulo;

    private DevolucionNoPermitidaException(String titulo, String mensaje) {
        super(mensaje);
        this.titulo = titulo;
    }

    /** FE-01 de CU-13 (RN-03): el cliente ya tiene 2 devoluciones en los últimos 6 meses. */
    public static DevolucionNoPermitidaException limiteAlcanzado(long cantidad, int limite, int meses) {
        return new DevolucionNoPermitidaException("Límite de devoluciones alcanzado",
                "No es posible iniciar una nueva solicitud porque se alcanzó el límite de " + limite
                        + " devoluciones en un período de " + meses + " meses (el cliente tiene " + cantidad + ").");
    }

    /** Precondición de CU-13 (RN-01): solo se devuelven productos de pedidos entregados. */
    public static DevolucionNoPermitidaException pedidoNoEntregado(Long idPedido, EstadoPedido estado) {
        return new DevolucionNoPermitidaException("El pedido no admite devolución",
                "El pedido " + idPedido + " está en estado " + estado + ". Solo se puede pedir la devolución de "
                        + "productos de pedidos entregados; antes de la entrega corresponde cancelar el pedido.");
    }

    /** Precondición de CU-13 (RN-02): pasaron más de 10 días corridos desde la entrega. */
    public static DevolucionNoPermitidaException fueraDePlazo(Long idPedido, int dias) {
        return new DevolucionNoPermitidaException("Plazo de devolución vencido",
                "El pedido " + idPedido + " se entregó hace más de " + dias + " días corridos. "
                        + "Pasado ese plazo no se puede iniciar la solicitud.");
    }

    /** CU-13 (RN-04): el producto ya tiene una solicitud pendiente de resolución. */
    public static DevolucionNoPermitidaException solicitudPendiente(Long idPedido, Long idProducto) {
        return new DevolucionNoPermitidaException("Ya existe una solicitud",
                "El producto " + idProducto + " del pedido " + idPedido
                        + " ya tiene una solicitud de devolución pendiente de resolución.");
    }

    /** FE-01 de CU-22 (RN-01): la solicitud no está «En tránsito a devolución». */
    public static DevolucionNoPermitidaException noAdmiteActualizacion(Long idSolicitud, EstadoDevolucion estado) {
        return new DevolucionNoPermitidaException("La solicitud no admite actualización",
                "La solicitud " + idSolicitud + " está en estado " + estado + ". Solo se pueden actualizar "
                        + "devoluciones aceptadas cuyo producto fue enviado o entregado en la empresa "
                        + "(estado EN_TRANSITO_A_DEVOLUCION).");
    }

    public String getTitulo() {
        return titulo;
    }
}
