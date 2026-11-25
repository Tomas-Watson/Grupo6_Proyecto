package com.aydsii.demo.service;

import com.aydsii.demo.domain.Cliente;
import com.aydsii.demo.domain.MedioPago;
import com.aydsii.demo.domain.PoliticaDevolucion;
import com.aydsii.demo.domain.SolicitudDevolucion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * «Service» NotificacionService de los diagramas de clases de diseño de CU-13 y CU-22.
 * <p>
 * En esta iteración el sistema no tiene correo ni notificaciones push, así que cada notificación
 * se <b>registra con el logger</b> (SLF4J). En {@code log4j2-spring.xml} este logger tiene su propio
 * archivo, {@code logs/notificaciones.log}: es un ejemplo de que el destino de los mensajes se
 * cambia en la configuración, sin tocar el código.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    /** Paso 6 de CU-13: avisa a Ventas que hay una nueva solicitud. */
    public void notificarNuevaSolicitud(SolicitudDevolucion solicitud) {
        log.info("[Para Ventas] Nueva solicitud de devolución N° {}: cliente {}, pedido {}, producto '{}'. "
                        + "Hay {} horas hábiles para responderla.",
                solicitud.getIdSolicitud(), cliente(solicitud).getNombre(), solicitud.getIdPedido(),
                solicitud.getNombreProducto(), PoliticaDevolucion.obtenerInstancia().getHorasRespuestaVentas());
    }

    /** Paso 10 de CU-22: le informa al cliente el resultado, el monto y el medio de reembolso. */
    public void notificarResultadoDevolucion(SolicitudDevolucion solicitud) {
        log.info("[Para {} <{}>] Tu devolución N° {} fue resuelta: el producto '{}' llegó en buen estado. "
                        + "Se te reembolsan ${} por {}.",
                cliente(solicitud).getNombre(), cliente(solicitud).getEmail(), solicitud.getIdSolicitud(),
                solicitud.getNombreProducto(), solicitud.getMontoAReembolsar(), medio(solicitud.medioReembolso()));
    }

    /** FA-01 de CU-22: el producto llegó dañado; se le informan al cliente sus dos opciones. */
    public void notificarProductoDanado(SolicitudDevolucion solicitud) {
        BigDecimal porcentaje = PoliticaDevolucion.obtenerInstancia().getPorcentajeDanadoSiLoDeja()
                .multiply(BigDecimal.valueOf(100)).stripTrailingZeros();
        log.info("[Para {} <{}>] Tu devolución N° {}: el producto '{}' llegó dañado. Podés dejarlo en la empresa "
                        + "y recibir el {}% del valor pagado, o pedir que te lo reenvíen sin reembolso y pagando "
                        + "el envío. Indicanos qué preferís.",
                cliente(solicitud).getNombre(), cliente(solicitud).getEmail(), solicitud.getIdSolicitud(),
                solicitud.getNombreProducto(), porcentaje.toPlainString());
    }

    private static Cliente cliente(SolicitudDevolucion solicitud) {
        return solicitud.obtenerDetalle().obtenerPedido().getCliente();
    }

    private static String medio(MedioPago medioPago) {
        return medioPago == null ? "el medio de pago original" : medioPago.name();
    }
}
