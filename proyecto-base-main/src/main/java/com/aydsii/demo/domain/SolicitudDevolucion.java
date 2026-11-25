package com.aydsii.demo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * «Entity» SolicitudDevolucion de los diagramas de clases de diseño de Solicitar devolución (CU-13)
 * y Actualizar devolución (CU-22) ({solicitud_devolucion} del DER). Es una sola clase para los dos
 * casos de uso, igual que {@link Pedido} en la primera iteración.
 * <p>
 * La solicitud se hace sobre un renglón del pedido ({@link DetallePedido}): así queda asociada al
 * pedido y al producto que se devuelve (RN-04 de CU-13).
 */
@Entity
@Table(name = "solicitud_devolucion")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"idSolicitud", "estado", "fechaSolicitud", "idPedido", "producto", "motivo", "fotoProducto",
        "fechaRecepcion", "resultadoVerificacion", "montoAReembolsar", "resolutor", "fechaResolucion"})
@Schema(description = "Solicitud de devolución de un producto de un pedido entregado")
public class SolicitudDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    @Schema(description = "Número de solicitud (número de devolución)", example = "7")
    private Long idSolicitud;

    /** Renglón del pedido sobre el que se pide la devolución (pedido + producto). */
    @ManyToOne(optional = false)
    @JoinColumns({
            @JoinColumn(name = "id_pedido", referencedColumnName = "id_pedido", nullable = false),
            @JoinColumn(name = "id_producto", referencedColumnName = "id_producto", nullable = false)})
    @JsonIgnore
    private DetallePedido detallePedido;

    @Column(name = "fecha_solicitud", nullable = false)
    @Schema(description = "Fecha y hora de la solicitud", example = "2026-11-25T10:30:00")
    private LocalDateTime fechaSolicitud;

    @Column(name = "motivo", length = 300)
    @Schema(description = "Motivo indicado por el cliente (opcional)", example = "Llegó con una rajadura")
    private String motivo;

    @Column(name = "foto_producto", nullable = false)
    @Schema(description = "Foto del producto adjuntada por el cliente", example = "fotos/maceta-pedido-7.jpg")
    private String fotoProducto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Schema(description = "Estado de la solicitud", example = "PENDIENTE_DE_REVISION")
    private EstadoDevolucion estado;

    @Column(name = "fecha_recepcion")
    @Schema(description = "Fecha y hora en que el producto llegó al depósito (CU-22)")
    private LocalDateTime fechaRecepcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_verificacion")
    @Schema(description = "Resultado de la verificación del estado físico (CU-22)")
    private ResultadoVerificacion resultadoVerificacion;

    @Column(name = "monto_a_reembolsar", precision = 12, scale = 2)
    @Schema(description = "Monto a reembolsar (CU-22, solo si el producto llegó en buen estado)")
    private BigDecimal montoAReembolsar;

    /** Empleado de Ventas que resolvió la devolución (CU-22). */
    @ManyToOne
    @JoinColumn(name = "id_resolutor")
    @JsonIgnore
    private Empleado resolutor;

    @Column(name = "fecha_resolucion")
    @Schema(description = "Fecha y hora en que se resolvió la devolución (CU-22)")
    private LocalDateTime fechaResolucion;

    /** Constructor requerido por JPA. */
    protected SolicitudDevolucion() {
    }

    /** CU-13: la solicitud nace en «Pendiente de revisión» (paso 5). */
    public SolicitudDevolucion(DetallePedido detallePedido, LocalDateTime fechaSolicitud, String motivo,
                               String fotoProducto) {
        this.detallePedido = detallePedido;
        this.fechaSolicitud = fechaSolicitud;
        this.motivo = motivo;
        this.fotoProducto = fotoProducto;
        this.estado = EstadoDevolucion.PENDIENTE_DE_REVISION;
    }

    // ------------------------------------------------------------ CU-22 Actualizar devolución

    /** RN-01 de CU-22: solo se actualizan las solicitudes «En tránsito a devolución». */
    public boolean admiteActualizacion() {
        return estado == EstadoDevolucion.EN_TRANSITO_A_DEVOLUCION;
    }

    /** Paso 7 de CU-22: el producto llegó al depósito. */
    public void registrarRecepcion() {
        this.fechaRecepcion = ahora();
    }

    /** Paso 7 de CU-22: resultado de la verificación del estado físico. */
    public void registrarResultado(ResultadoVerificacion resultado) {
        this.resultadoVerificacion = resultado;
    }

    /**
     * Pasos 8 y 9 de CU-22: calcula el monto a reembolsar y deja la devolución «Resuelta», con el
     * empleado de Ventas que la resolvió.
     *
     * @throws IllegalStateException si el producto no llegó en buen estado
     */
    public void resolver(Empleado resolutor) {
        if (resultadoVerificacion != ResultadoVerificacion.BUEN_ESTADO) {
            throw new IllegalStateException("Solo se resuelve en este caso de uso si el producto llegó en buen estado");
        }
        this.montoAReembolsar = calcular();
        this.resolutor = resolutor;
        this.estado = EstadoDevolucion.RESUELTA;
        this.fechaResolucion = ahora();
    }

    /** FA-01 de CU-22: el producto llegó dañado y queda a la espera de la decisión del cliente. */
    public void quedarEnVerificacion() {
        this.estado = EstadoDevolucion.EN_VERIFICACION;
    }

    /** Paso 8 de CU-22: 100 % del valor pagado por el producto. Usa el Singleton de la política. */
    private BigDecimal calcular() {
        return PoliticaDevolucion.obtenerInstancia().calcularReembolsoBuenEstado(detallePedido.getSubtotal());
    }

    /** RN-06 de CU-22: el reembolso se hace por el mismo medio de pago original. */
    public MedioPago medioReembolso() {
        return detallePedido.obtenerPedido().medioPagoOriginal();
    }

    public DetallePedido obtenerDetalle() {
        return detallePedido;
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    // ------------------------------------------------------------ Datos que se muestran en el JSON

    @JsonProperty("idPedido")
    @Schema(description = "Número del pedido", example = "7")
    public Long getIdPedido() {
        return detallePedido.obtenerIdPedido();
    }

    @JsonProperty("producto")
    @Schema(description = "Producto que se devuelve", example = "Maceta mediana de PET reciclado")
    public String getNombreProducto() {
        return detallePedido.getProducto().getNombre();
    }

    @JsonProperty("resolutor")
    @Schema(description = "Empleado de Ventas que resolvió la devolución", example = "Valeria Torres")
    public String getNombreResolutor() {
        return resolutor == null ? null : resolutor.getNombre();
    }

    // ------------------------------------------------------------ Getters

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getFotoProducto() {
        return fotoProducto;
    }

    public EstadoDevolucion getEstado() {
        return estado;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public ResultadoVerificacion getResultadoVerificacion() {
        return resultadoVerificacion;
    }

    public BigDecimal getMontoAReembolsar() {
        return montoAReembolsar;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }
}
