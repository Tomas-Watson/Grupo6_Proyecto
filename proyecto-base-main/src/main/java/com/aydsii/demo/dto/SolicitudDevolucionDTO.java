package com.aydsii.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * «DTO» SolicitudDevolucionDTO del diagrama de clases de diseño de CU-13: los datos que el cliente
 * envía al confirmar la solicitud (pasos 1 y 3). El número de pedido viaja en la URL.
 */
@Schema(description = "Datos de la solicitud de devolución que envía el cliente")
public record SolicitudDevolucionDTO(

        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser un número entero positivo")
        @Schema(description = "Producto del pedido que se quiere devolver (paso 1)", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long idProducto,

        @Size(max = 300, message = "admite hasta 300 caracteres")
        @Schema(description = "Motivo de la devolución (opcional, paso 3)", example = "Llegó con una rajadura",
                nullable = true)
        String motivo,

        @NotBlank(message = "es obligatoria")
        @Size(max = 255, message = "admite hasta 255 caracteres")
        @Schema(description = "Foto del producto (paso 3): nombre o URL del archivo adjunto",
                example = "fotos/maceta-pedido-7.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
        String fotoProducto) {
}
