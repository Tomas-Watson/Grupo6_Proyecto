package com.aydsii.demo.repository;

import com.aydsii.demo.domain.SolicitudDevolucion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * «Repository» SolicitudDevolucionRepository de los diagramas de clases de diseño de CU-13 y CU-22.
 * Como los repositorios de la primera iteración, extiende
 * {@link org.springframework.data.repository.Repository} para exponer solo los métodos de los diagramas.
 */
@Repository
public interface SolicitudDevolucionRepository
        extends org.springframework.data.repository.Repository<SolicitudDevolucion, Long> {

    /** CU-13 registra la solicitud; CU-22 guarda la actualización. */
    SolicitudDevolucion save(SolicitudDevolucion solicitud);

    /** CU-22: busca la solicitud por su número de devolución. */
    Optional<SolicitudDevolucion> findById(Long idSolicitud);

    /**
     * CU-13, RN-03: devoluciones del cliente pedidas desde una fecha (las rechazadas no cuentan).
     * El cliente se llega por solicitud → detalle → pedido → cliente.
     */
    @Query("""
            select count(s) from SolicitudDevolucion s
            where s.detallePedido.pedido.cliente.idUsuario = :idCliente
              and s.fechaSolicitud > :desde
              and s.estado <> com.aydsii.demo.domain.EstadoDevolucion.RECHAZADA
            """)
    long countByClienteIdAndFechaSolicitudAfter(@Param("idCliente") Long idCliente,
                                                @Param("desde") LocalDateTime desde);

    /**
     * CU-13, RN-04: indica si el producto de ese pedido ya tiene una solicitud pendiente de
     * resolución (ni resuelta ni rechazada).
     */
    @Query("""
            select count(s) > 0 from SolicitudDevolucion s
            where s.detallePedido.pedido.idPedido = :idPedido
              and s.detallePedido.producto.idProducto = :idProducto
              and s.estado not in (com.aydsii.demo.domain.EstadoDevolucion.RESUELTA,
                                   com.aydsii.demo.domain.EstadoDevolucion.RECHAZADA)
            """)
    boolean existsPendienteByPedidoIdAndProductoId(@Param("idPedido") Long idPedido,
                                                   @Param("idProducto") Long idProducto);
}
