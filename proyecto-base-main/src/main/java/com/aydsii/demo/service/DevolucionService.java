package com.aydsii.demo.service;

import com.aydsii.demo.domain.Cliente;
import com.aydsii.demo.domain.DetallePedido;
import com.aydsii.demo.domain.Empleado;
import com.aydsii.demo.domain.EstadoPedido;
import com.aydsii.demo.domain.Pedido;
import com.aydsii.demo.domain.PoliticaDevolucion;
import com.aydsii.demo.domain.ResultadoVerificacion;
import com.aydsii.demo.domain.SolicitudDevolucion;
import com.aydsii.demo.dto.DetalleDevolucionDTO;
import com.aydsii.demo.dto.SolicitudDevolucionDTO;
import com.aydsii.demo.dto.VerificacionDTO;
import com.aydsii.demo.repository.EmpleadoRepository;
import com.aydsii.demo.repository.PedidoRepository;
import com.aydsii.demo.repository.SolicitudDevolucionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * «Service» DevolucionService de los diagramas de clases de diseño de Solicitar devolución (CU-13)
 * y Actualizar devolución (CU-22). Los comentarios indican el paso de la especificación (y el
 * flujo alternativo o de excepción) que implementa cada línea.
 * <p>
 * Usa los dos patrones de la segunda iteración:
 * <ul>
 *   <li><b>Simple Logging Facade (SLF4J)</b>: el {@code log} de abajo. La clase solo conoce la API de
 *       SLF4J; el motor que escribe los mensajes (Log4j2) se elige en el pom y se configura en
 *       {@code log4j2-spring.xml}.</li>
 *   <li><b>Singleton</b>: {@link PoliticaDevolucion#obtenerInstancia()} da los plazos y límites del
 *       protocolo de devolución.</li>
 * </ul>
 */
@Service
public class DevolucionService {

    private static final Logger log = LoggerFactory.getLogger(DevolucionService.class);

    private final SolicitudDevolucionRepository solicitudDevolucionRepository;
    private final PedidoRepository pedidoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final NotificacionService notificacionService;

    public DevolucionService(SolicitudDevolucionRepository solicitudDevolucionRepository,
                             PedidoRepository pedidoRepository,
                             EmpleadoRepository empleadoRepository,
                             NotificacionService notificacionService) {
        this.solicitudDevolucionRepository = solicitudDevolucionRepository;
        this.pedidoRepository = pedidoRepository;
        this.empleadoRepository = empleadoRepository;
        this.notificacionService = notificacionService;
    }

    // =====================================================================================
    // CU-13 Solicitar devolución de producto
    // =====================================================================================

    /**
     * Pasos 4 a 7: el cliente ya eligió el producto, completó el motivo y la foto y confirmó.
     * <p>
     * FA-01 (el cliente no confirma) no llega al servicio: si no confirma, el front no lo invoca
     * y no hay nada que deshacer. Todo ocurre en una transacción: si algo falla, no queda ninguna
     * solicitud registrada (postcondición de fallo).
     *
     * @throws RecursoNoEncontradoException   si el pedido no existe o el producto no está en el pedido
     * @throws DevolucionNoPermitidaException si no se cumple una precondición o el cliente alcanzó el límite (FE-01)
     */
    @Transactional
    public SolicitudDevolucion solicitarDevolucion(Long idPedido, SolicitudDevolucionDTO datos) {
        log.debug("CU-13: solicitud de devolución del producto {} del pedido {}", datos.idProducto(), idPedido);

        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> RecursoNoEncontradoException.pedido(idPedido));
        verificarPedidoDevolvible(pedido);                                      // precondiciones (RN-01, RN-02)

        Long idCliente = pedido.obtenerIdCliente();
        verificarLimiteDevoluciones(idCliente);                                 // paso 4 (FE-01)

        DetallePedido detalle = pedido.obtenerDetalle(datos.idProducto());
        if (detalle == null) {
            throw RecursoNoEncontradoException.productoEnPedido(idPedido, datos.idProducto());
        }
        if (solicitudDevolucionRepository.existsPendienteByPedidoIdAndProductoId(idPedido, datos.idProducto())) {
            log.warn("RN-04: el producto {} del pedido {} ya tiene una solicitud pendiente", datos.idProducto(), idPedido);
            throw DevolucionNoPermitidaException.solicitudPendiente(idPedido, datos.idProducto());
        }

        SolicitudDevolucion solicitud = new SolicitudDevolucion(detalle, ahora(),
                datos.motivo(), datos.fotoProducto());
        SolicitudDevolucion solicitudGuardada = solicitudDevolucionRepository.save(solicitud);   // paso 5
        notificacionService.notificarNuevaSolicitud(solicitudGuardada);                          // paso 6

        log.info("CU-13: solicitud de devolución N° {} registrada para el cliente {} (pedido {}, producto {})",
                solicitudGuardada.getIdSolicitud(), idCliente, idPedido, datos.idProducto());
        return solicitudGuardada;                                                                // paso 7
    }

    /**
     * Precondiciones (RN-01 y RN-02): el listado de ítems del pedido solo habilita «devolver» en
     * pedidos entregados hace 10 días o menos, pero el servicio lo vuelve a controlar por si se lo
     * invoca directamente.
     */
    private void verificarPedidoDevolvible(Pedido pedido) {
        PoliticaDevolucion politica = PoliticaDevolucion.obtenerInstancia();
        if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
            log.warn("RN-01: el pedido {} está {} y no admite devolución", pedido.getIdPedido(), pedido.getEstado());
            throw DevolucionNoPermitidaException.pedidoNoEntregado(pedido.getIdPedido(), pedido.getEstado());
        }
        if (!politica.dentroDelPlazo(pedido.getFechaEntrega(), ahora())) {
            log.warn("RN-02: el pedido {} se entregó el {}, fuera del plazo de {} días",
                    pedido.getIdPedido(), pedido.getFechaEntrega(), politica.getPlazoDiasDevolucion());
            throw DevolucionNoPermitidaException.fueraDePlazo(pedido.getIdPedido(), politica.getPlazoDiasDevolucion());
        }
    }

    /** Paso 4 (RN-03): el cliente no puede superar 2 devoluciones en 6 meses. Si las alcanzó, FE-01. */
    private void verificarLimiteDevoluciones(Long idCliente) {
        PoliticaDevolucion politica = PoliticaDevolucion.obtenerInstancia();
        long cantidad = solicitudDevolucionRepository.countByClienteIdAndFechaSolicitudAfter(
                idCliente, politica.inicioPeriodoLimite(ahora()));
        log.debug("RN-03: el cliente {} tiene {} devoluciones en los últimos {} meses",
                idCliente, cantidad, politica.getMesesPeriodoLimite());
        if (cantidad >= politica.getLimiteDevoluciones()) {
            log.warn("FE-01: el cliente {} alcanzó el límite de {} devoluciones", idCliente, politica.getLimiteDevoluciones());
            throw DevolucionNoPermitidaException.limiteAlcanzado(cantidad, politica.getLimiteDevoluciones(),
                    politica.getMesesPeriodoLimite());
        }
    }

    // =====================================================================================
    // CU-22 Actualizar devolución
    // =====================================================================================

    /**
     * Servicio 1, pasos 3 a 5: Ventas ingresa el número de devolución y el sistema le muestra los
     * datos de la solicitud. No registra nada: si Ventas no sigue (FA-02), la solicitud queda igual.
     *
     * @throws RecursoNoEncontradoException   si la solicitud no existe (FE-01)
     * @throws DevolucionNoPermitidaException si la solicitud no admite actualización (FE-01)
     */
    @Transactional(readOnly = true)
    public DetalleDevolucionDTO obtenerDevolucion(Long idSolicitud) {
        SolicitudDevolucion solicitud = buscarSolicitudActualizable(idSolicitud);   // paso 4
        DetallePedido detalle = solicitud.obtenerDetalle();
        Pedido pedido = detalle.obtenerPedido();
        Cliente cliente = pedido.getCliente();
        log.debug("CU-22: se muestran los datos de la devolución N° {}", idSolicitud);
        return new DetalleDevolucionDTO(solicitud, detalle, pedido, cliente);       // paso 5
    }

    /**
     * Servicio 2, pasos 6 a 10: Ventas indica el resultado de la verificación y confirma.
     * Recién acá se registra la recepción (fecha y hora), como pidió la profesora.
     * Con BUEN_ESTADO se calcula el monto y la devolución queda «Resuelta»; con DANADO queda
     * «En verificación» a la espera de la decisión del cliente (FA-01).
     *
     * @throws RecursoNoEncontradoException   si la solicitud o el empleado no existen
     * @throws DevolucionNoPermitidaException si la solicitud no admite actualización (FE-01)
     * @throws AccesoDenegadoException        si el empleado no es de Ventas
     */
    @Transactional
    public SolicitudDevolucion actualizarDevolucion(Long idSolicitud, VerificacionDTO datos, Long idEmpleado) {
        SolicitudDevolucion solicitud = buscarSolicitudActualizable(idSolicitud);   // se vuelve a verificar
        Empleado resolutor = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> RecursoNoEncontradoException.empleado(idEmpleado));
        if (!resolutor.esVentas()) {
            log.warn("El empleado {} no es de Ventas y no puede actualizar la devolución {}", idEmpleado, idSolicitud);
            throw AccesoDenegadoException.noEsVentas(idEmpleado);
        }

        solicitud.registrarRecepcion();                                             // paso 7
        solicitud.registrarResultado(datos.resultado());                            // paso 7

        SolicitudDevolucion solicitudGuardada;
        if (datos.resultado() == ResultadoVerificacion.BUEN_ESTADO) {
            solicitud.resolver(resolutor);                                          // pasos 8 y 9
            solicitudGuardada = solicitudDevolucionRepository.save(solicitud);
            notificacionService.notificarResultadoDevolucion(solicitudGuardada);    // paso 10
            log.info("CU-22: devolución N° {} resuelta por {}. Monto a reembolsar: {}",
                    idSolicitud, resolutor.getNombre(), solicitudGuardada.getMontoAReembolsar());
        } else {
            solicitud.quedarEnVerificacion();                                       // FA-01
            solicitudGuardada = solicitudDevolucionRepository.save(solicitud);
            notificacionService.notificarProductoDanado(solicitudGuardada);         // FA-01
            log.info("CU-22 FA-01: la devolución N° {} llegó dañada y queda en verificación", idSolicitud);
        }
        return solicitudGuardada;                                                   // paso 10
    }

    /** Paso 4 de CU-22: la solicitud existe y está «En tránsito a devolución». Si no, FE-01. */
    private SolicitudDevolucion buscarSolicitudActualizable(Long idSolicitud) {
        SolicitudDevolucion solicitud = solicitudDevolucionRepository.findById(idSolicitud)
                .orElseThrow(() -> {
                    log.warn("FE-01: no existe la solicitud de devolución {}", idSolicitud);
                    return RecursoNoEncontradoException.solicitud(idSolicitud);
                });
        if (!solicitud.admiteActualizacion()) {
            log.warn("FE-01: la solicitud {} está {} y no admite actualización", idSolicitud, solicitud.getEstado());
            throw DevolucionNoPermitidaException.noAdmiteActualizacion(idSolicitud, solicitud.getEstado());
        }
        return solicitud;
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
