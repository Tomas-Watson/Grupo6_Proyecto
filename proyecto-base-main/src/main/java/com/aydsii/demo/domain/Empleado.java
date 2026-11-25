package com.aydsii.demo.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * «Entity» Empleado del diagrama de clases de diseño de Actualizar devolución (CU-22)
 * ({empleado} del DER). Igual que {@link Cliente}, es un {@link Usuario}: {empleado.id_empleado}
 * es a la vez clave primaria y clave foránea a {usuario.id_usuario}.
 */
@Entity
@Table(name = "empleado")
@PrimaryKeyJoinColumn(name = "id_empleado")
@DiscriminatorValue("EMPLEADO")
@JsonPropertyOrder({"idUsuario", "nombre", "email", "rol"})
@Schema(description = "Empleado de VerdeGreen")
public class Empleado extends Usuario {

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    @Schema(description = "Rol del empleado", example = "VENTAS")
    private RolEmpleado rol;

    /** Constructor requerido por JPA. */
    protected Empleado() {
    }

    /** Solo un empleado de Ventas puede actualizar una devolución (precondición de CU-22). */
    public boolean esVentas() {
        return rol == RolEmpleado.VENTAS;
    }

    public RolEmpleado getRol() {
        return rol;
    }
}
