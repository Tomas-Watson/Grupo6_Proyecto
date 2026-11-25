package com.aydsii.demo.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/** Medio con el que se pagó el pedido ({pago.medio_pago} del DER). El reembolso se hace por el mismo medio. */
@Schema(description = "Medio de pago del pedido; el reembolso se hace por el mismo medio")
public enum MedioPago {
    TARJETA_CREDITO,
    BILLETERA_VIRTUAL,
    TRANSFERENCIA,
    EFECTIVO
}
