package com.aydsii.demo.controller;

import com.aydsii.demo.domain.EstadoDevolucion;
import com.aydsii.demo.domain.PoliticaDevolucion;
import com.aydsii.demo.domain.SolicitudDevolucion;
import com.aydsii.demo.dto.ApiResponseDTO;
import com.aydsii.demo.dto.DetalleDevolucionDTO;
import com.aydsii.demo.dto.ErrorDTO;
import com.aydsii.demo.dto.SolicitudDevolucionDTO;
import com.aydsii.demo.dto.VerificacionDTO;
import com.aydsii.demo.service.DevolucionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * «Controller» DevolucionController de los diagramas de clases de diseño de Solicitar devolución
 * (CU-13) y Actualizar devolución (CU-22). Igual que {@link PedidoController}, delega toda la
 * lógica en el servicio y arma la respuesta con {@link ApiResponseDTO}; los errores los traduce
 * {@link ManejadorGlobalExcepciones}.
 */
@RestController
@RequestMapping("/api/devoluciones")
public class DevolucionController {

    static final String TAG_SOLICITAR = "3. Solicitar devolución (CU-13)";
    static final String TAG_ACTUALIZAR = "4. Actualizar devolución (CU-22)";

    static final String DESCRIPCION_SOLICITAR = """
            Caso de uso Solicitar devolución de producto (CU-13). Datos de prueba:

            | Flujo | idPedido | idProducto | Resultado |
            |---|---|---|---|
            | Camino feliz | 7 | 1 o 2 | 201, solicitud «Pendiente de revisión» |
            | RN-04 (repetir el mismo producto) | 7 | 1 | 409, ya existe una solicitud |
            | FE-01 (límite de 2 en 6 meses) | 8 | 3 | 409, límite alcanzado |
            | RN-02 (entregado hace más de 10 días) | 5 | 3 | 409, plazo vencido |
            | RN-01 (pedido no entregado) | 1 | 1 | 409, no admite devolución |
            | Producto que no está en el pedido | 7 | 5 | 404 |
            """;

    static final String DESCRIPCION_ACTUALIZAR = """
            Caso de uso Actualizar devolución (CU-22). Dos servicios: primero se consultan los datos
            (GET) y después se indica el resultado (PATCH). Ventas es el empleado 7. Datos de prueba:

            | Flujo | idSolicitud | Resultado |
            |---|---|---|
            | Camino feliz: GET y PATCH con BUEN_ESTADO | 3 | 200, «Resuelta» con monto |
            | FA-01: PATCH con DANADO | 4 | 200, «En verificación» |
            | FA-02: solo GET, sin PATCH | 6 | la solicitud no cambia |
            | FE-01: no admite actualización | 5 (o 3 ya resuelta) | 409 |
            | FE-01: no existe | 999 | 404 |
            | Empleado que no es de Ventas (idEmpleado 8) | 6 | 403 |
            """;

    private final DevolucionService devolucionService;

    public DevolucionController(DevolucionService devolucionService) {
        this.devolucionService = devolucionService;
    }

    // =====================================================================================
    // CU-13 Solicitar devolución de producto
    // =====================================================================================

    @PostMapping(value = "/pedidos/{idPedido}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(tags = TAG_SOLICITAR, operationId = "solicitarDevolucion",
            summary = "Pasos 3 a 7: el cliente confirma la solicitud de devolución de un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud registrada en PENDIENTE_DE_REVISION (paso 7)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Faltan datos o tienen un formato inválido (por ejemplo, sin foto)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "El pedido no existe o el producto no está en el pedido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "FE-01: límite de devoluciones alcanzado. También: pedido "
                    + "no entregado (RN-01), plazo vencido (RN-02) o solicitud ya existente (RN-04)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<SolicitudDevolucion>> solicitarDevolucion(
            @Parameter(description = "Número del pedido entregado", example = "7", required = true)
            @PathVariable("idPedido") @Positive Long idPedido,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                            @ExampleObject(name = "Camino feliz (pedido 7)",
                                    value = "{\"idProducto\": 1, \"motivo\": \"Llegó con una rajadura\", "
                                            + "\"fotoProducto\": \"fotos/maceta-pedido-7.jpg\"}"),
                            @ExampleObject(name = "FE-01: límite alcanzado (pedido 8)",
                                    value = "{\"idProducto\": 3, \"motivo\": null, "
                                            + "\"fotoProducto\": \"fotos/botella-pedido-8.jpg\"}")}))
            @Valid @RequestBody SolicitudDevolucionDTO datos) {
        SolicitudDevolucion solicitud = devolucionService.solicitarDevolucion(idPedido, datos);
        PoliticaDevolucion politica = PoliticaDevolucion.obtenerInstancia();
        String mensaje = "Solicitud registrada. Ventas la responderá dentro de las "
                + politica.getHorasRespuestaVentas() + " horas hábiles. " + politica.condiciones();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO<>(HttpStatus.CREATED.value(), mensaje, solicitud));
    }

    // =====================================================================================
    // CU-22 Actualizar devolución
    // =====================================================================================

    @GetMapping(value = "/{idSolicitud}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(tags = TAG_ACTUALIZAR, operationId = "obtenerDevolucion",
            summary = "Servicio 1, pasos 3 a 5: Ventas ingresa el número de devolución y ve sus datos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos de la solicitud (paso 5)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "FE-01: el número de devolución no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "FE-01: la solicitud no está EN_TRANSITO_A_DEVOLUCION",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<DetalleDevolucionDTO>> obtenerDevolucion(
            @Parameter(description = "Número de devolución", example = "3", required = true)
            @PathVariable("idSolicitud") @Positive Long idSolicitud) {
        DetalleDevolucionDTO dto = devolucionService.obtenerDevolucion(idSolicitud);
        String mensaje = "Datos de la devolución. Indicá el resultado de la verificación del estado físico "
                + "(BUEN_ESTADO o DANADO).";
        return ResponseEntity.ok(new ApiResponseDTO<>(HttpStatus.OK.value(), mensaje, dto));
    }

    @PatchMapping(value = "/{idSolicitud}/verificacion", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(tags = TAG_ACTUALIZAR, operationId = "actualizarDevolucion",
            summary = "Servicio 2, pasos 6 a 10: Ventas indica el resultado y confirma la actualización")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BUEN_ESTADO: queda RESUELTA con el monto a reembolsar. "
                    + "DANADO (FA-01): queda EN_VERIFICACION",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Falta el resultado o no es BUEN_ESTADO ni DANADO",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "El empleado no es de Ventas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "FE-01: el número de devolución no existe, o no existe el empleado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "FE-01: la solicitud no está EN_TRANSITO_A_DEVOLUCION",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<SolicitudDevolucion>> actualizarDevolucion(
            @Parameter(description = "Número de devolución", example = "3", required = true)
            @PathVariable("idSolicitud") @Positive Long idSolicitud,
            @Parameter(description = "Empleado de Ventas que actualiza la devolución", example = "7", required = true)
            @RequestParam("idEmpleado") @Positive Long idEmpleado,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                            @ExampleObject(name = "Buen estado (solicitud 3)", value = "{\"resultado\": \"BUEN_ESTADO\"}"),
                            @ExampleObject(name = "FA-01: dañado (solicitud 4)", value = "{\"resultado\": \"DANADO\"}")}))
            @Valid @RequestBody VerificacionDTO datos) {
        SolicitudDevolucion solicitud = devolucionService.actualizarDevolucion(idSolicitud, datos, idEmpleado);
        String mensaje = solicitud.getEstado() == EstadoDevolucion.RESUELTA
                ? "Devolución resuelta. Monto a reembolsar: $" + solicitud.getMontoAReembolsar()
                        + ". Se notificó al cliente."
                : "El producto llegó dañado: la devolución quedó en verificación a la espera de la decisión del "
                        + "cliente (FA-01). Se notificó al cliente.";
        return ResponseEntity.ok(new ApiResponseDTO<>(HttpStatus.OK.value(), mensaje, solicitud));
    }
}
