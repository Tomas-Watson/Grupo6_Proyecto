package com.aydsii.demo.dto;

import com.aydsii.demo.domain.Cliente;
import com.aydsii.demo.domain.DetallePedido;
import com.aydsii.demo.domain.EstadoDevolucion;
import com.aydsii.demo.domain.Pedido;
import com.aydsii.demo.domain.SolicitudDevolucion;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * «DTO» DetalleDevolucionDTO del diagrama de clases de diseño de CU-22: los datos de la solicitud
 * que el sistema le muestra a Ventas cuando ingresa el número de devolución (paso 5). Es la
 * respuesta del primer servicio.
 */
@JsonPropertyOrder({"idSolicitud", "estado", "fechaSolicitud", "cliente", "idPedido", "fechaEntrega", "producto",
        "valorPagado", "motivo", "fotoProducto"})
@Schema(description = "Datos de la solicitud de devolución que ve Ventas antes de indicar el resultado")
public record DetalleDevolucionDTO(

        @Schema(description = "Número de devolución", example = "3")
        Long idSolicitud,

        @Schema(description = "Estado de la solicitud", example = "EN_TRANSITO_A_DEVOLUCION")
        EstadoDevolucion estado,

        @Schema(description = "Fecha y hora de la solicitud", example = "2026-11-21T10:30:00")
        LocalDateTime fechaSolicitud,

        @Schema(description = "Cliente que pidió la devolución", example = "Bruno Díaz")
        String cliente,

        @Schema(description = "Número del pedido", example = "10")
        Long idPedido,

        @Schema(description = "Fecha y hora de entrega del pedido", example = "2026-11-17T10:30:00")
        LocalDateTime fechaEntrega,

        @Schema(description = "Producto que se devuelve", example = "Bandeja de PET reciclado")
        String producto,

        @Schema(description = "Valor pagado por el producto", example = "2800.00")
        BigDecimal valorPagado,

        @Schema(description = "Motivo indicado por el cliente (puede no tener)", example = "El color no coincide")
        String motivo,

        @Schema(description = "Foto del producto adjuntada por el cliente", example = "fotos/sol-3.jpg")
        String fotoProducto) {

    /** Mensaje «create» del diagrama de secuencia: arma el DTO con la solicitud, el detalle, el pedido y el cliente. */
    public DetalleDevolucionDTO(SolicitudDevolucion solicitud, DetallePedido detalle, Pedido pedido, Cliente cliente) {
        this(solicitud.getIdSolicitud(), solicitud.getEstado(), solicitud.getFechaSolicitud(), cliente.getNombre(),
                pedido.getIdPedido(), pedido.getFechaEntrega(), detalle.getProducto().getNombre(),
                detalle.getSubtotal(), solicitud.getMotivo(), solicitud.getFotoProducto());
    }
}
