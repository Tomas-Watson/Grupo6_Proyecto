package com.aydsii.demo.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado de la verificación del estado físico del producto devuelto (RN-03 de CU-22). */
@Schema(description = "Resultado de la verificación del estado físico: BUEN_ESTADO o DANADO")
public enum ResultadoVerificacion {
    BUEN_ESTADO,
    DANADO
}
