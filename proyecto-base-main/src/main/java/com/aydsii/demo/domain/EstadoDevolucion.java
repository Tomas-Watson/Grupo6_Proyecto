package com.aydsii.demo.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estados de una solicitud de devolución, según las características de una Solicitud de Devolución
 * del informe de requerimientos.
 * <ul>
 *   <li>Solicitar devolución (CU-13) crea la solicitud en {@link #PENDIENTE_DE_REVISION}.</li>
 *   <li>Actualizar devolución (CU-22) solo trabaja con solicitudes {@link #EN_TRANSITO_A_DEVOLUCION} y las deja
 *       {@link #RESUELTA} (buen estado) o {@link #EN_VERIFICACION} (dañado, espera la decisión del cliente).</li>
 * </ul>
 */
@Schema(description = "Estado de la solicitud de devolución")
public enum EstadoDevolucion {
    PENDIENTE_DE_REVISION,
    RECHAZADA,
    ACEPTADA,
    EN_TRANSITO_A_DEVOLUCION,
    EN_VERIFICACION,
    RESUELTA
}
