package com.aydsii.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * «Entity» Pago del diagrama de clases de diseño de Actualizar devolución (CU-22) ({pago} del DER).
 * Solo se mapea lo que usa el caso de uso: el medio de pago, porque el reembolso se hace por el
 * mismo medio de pago original (RN-06). Esta iteración solo lo lee, no registra pagos.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @Column(name = "id_pago")
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;

    /** Constructor requerido por JPA. */
    protected Pago() {
    }

    public Long getIdPago() {
        return idPago;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }
}
