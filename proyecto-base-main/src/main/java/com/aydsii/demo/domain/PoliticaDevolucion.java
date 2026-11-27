package com.aydsii.demo.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Patrón <b>Singleton</b>: la política de devoluciones de VerdeGreen (Protocolo de Devolución del
 * informe de requerimientos). Reúne en un único objeto los valores de las reglas de negocio de
 * Solicitar devolución (CU-13) y Actualizar devolución (CU-22).
 * <p>
 * Por qué Singleton: tiene que haber <b>una sola</b> política vigente en todo el sistema y la
 * necesitan clases de distintas capas, incluida la entidad {@link SolicitudDevolucion}, que no es
 * un bean de Spring y por eso no puede recibir dependencias inyectadas. El Singleton le da a todas
 * un punto de acceso global ({@link #obtenerInstancia()}) a la misma instancia.
 * <p>
 * Se implementa como en el apunte de patrones:
 * <ul>
 *   <li>atributo estático privado {@code instancia} con la única instancia;</li>
 *   <li>constructor privado, para que nadie pueda hacer {@code new PoliticaDevolucion()};</li>
 *   <li>método estático {@code obtenerInstancia()} que la crea la primera vez y después siempre
 *       devuelve la misma. Es {@code synchronized} para que dos pedidos simultáneos no creen dos
 *       instancias.</li>
 * </ul>
 */
public final class PoliticaDevolucion {

    private static final Logger log = LoggerFactory.getLogger(PoliticaDevolucion.class);

    /** La única instancia de la clase. */
    private static PoliticaDevolucion instancia;

    /** RN-02 de CU-13: días corridos desde la entrega para pedir la devolución. */
    private final int plazoDiasDevolucion;
    /** RN-03 de CU-13: devoluciones permitidas por cliente... */
    private final int limiteDevoluciones;
    /** ...dentro de este período, en meses. */
    private final int mesesPeriodoLimite;
    /** RN-05 de CU-13: horas hábiles que tiene Ventas para responder la solicitud. */
    private final int horasRespuestaVentas;
    /** RN-04 de CU-22: porcentaje del valor pagado que se reembolsa si el producto llegó en buen estado. */
    private final BigDecimal porcentajeBuenEstado;
    /** RN-04 de CU-22: porcentaje que se reembolsa si llegó dañado y el cliente lo deja en la empresa. */
    private final BigDecimal porcentajeDanadoSiLoDeja;

    /** Constructor privado: solo lo usa {@link #obtenerInstancia()}. */
    // nadie afuera de la clase puede hacer un new()
    private PoliticaDevolucion() {
        this.plazoDiasDevolucion = 10;
        this.limiteDevoluciones = 2;
        this.mesesPeriodoLimite = 6;
        this.horasRespuestaVentas = 48;
        this.porcentajeBuenEstado = new BigDecimal("1.00");
        this.porcentajeDanadoSiLoDeja = new BigDecimal("0.50");
        log.info("Singleton: se creó la única instancia de PoliticaDevolucion");
    }

    /** Punto de acceso global: crea la instancia la primera vez y después devuelve siempre la misma. */
    public static synchronized PoliticaDevolucion obtenerInstancia() { // synchronized para atender varias peticiones 
        // a la vez en disitintos hilos, evitando que dos peticiones lleguen al mismo tiempo y una pueda ver null
                                                                        
        if (instancia == null) {
            instancia = new PoliticaDevolucion();
        }
        return instancia;
    }

    /** RN-02 de CU-13: el pedido se entregó hace 10 días corridos o menos. */
    public boolean dentroDelPlazo(LocalDateTime fechaEntrega, LocalDateTime ahora) {
        return fechaEntrega != null && !fechaEntrega.plusDays(plazoDiasDevolucion).isBefore(ahora);
    }

    /** RN-03 de CU-13: desde cuándo se cuentan las devoluciones del cliente (hace 6 meses). */
    public LocalDateTime inicioPeriodoLimite(LocalDateTime ahora) {
        return ahora.minusMonths(mesesPeriodoLimite);
    }

    /** RN-04 de CU-22: reembolso cuando el producto llegó en buen estado (100 % del valor pagado). */
    public BigDecimal calcularReembolsoBuenEstado(BigDecimal valorPagado) {
        return valorPagado.multiply(porcentajeBuenEstado).setScale(2, RoundingMode.HALF_UP);
    }

    /** Condiciones de la devolución que se le informan al cliente (paso 7 de CU-13). */
    public String condiciones() {
        return "El envío del producto a la empresa corre por cuenta de VerdeGreen; el producto se verifica "
                + "al recibirse y el reembolso depende del resultado de esa verificación.";
    }

    public int getPlazoDiasDevolucion() {
        return plazoDiasDevolucion;
    }

    public int getLimiteDevoluciones() {
        return limiteDevoluciones;
    }

    public int getMesesPeriodoLimite() {
        return mesesPeriodoLimite;
    }

    public int getHorasRespuestaVentas() {
        return horasRespuestaVentas;
    }

    public BigDecimal getPorcentajeDanadoSiLoDeja() {
        return porcentajeDanadoSiLoDeja;
    }
}
