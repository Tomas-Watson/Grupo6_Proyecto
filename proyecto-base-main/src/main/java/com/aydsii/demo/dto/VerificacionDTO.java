package com.aydsii.demo.dto;

import com.aydsii.demo.domain.ResultadoVerificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** «DTO» VerificacionDTO del diagrama de clases de diseño de CU-22: el resultado que indica Ventas (paso 6). */
@Schema(description = "Resultado de la verificación del estado físico que indica Ventas")
public record VerificacionDTO(

        @NotNull(message = "es obligatorio: BUEN_ESTADO o DANADO")
        @Schema(description = "BUEN_ESTADO (flujo principal) o DANADO (FA-01)", example = "BUEN_ESTADO",
                requiredMode = Schema.RequiredMode.REQUIRED)
        ResultadoVerificacion resultado) {
}
