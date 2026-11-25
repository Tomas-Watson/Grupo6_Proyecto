package com.aydsii.demo.repository;

import com.aydsii.demo.domain.Empleado;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** «Repository» EmpleadoRepository del diagrama de clases de diseño de CU-22 Actualizar devolución. */
@Repository
public interface EmpleadoRepository extends org.springframework.data.repository.Repository<Empleado, Long> {

    /** CU-22: el empleado de Ventas que resuelve la devolución. */
    Optional<Empleado> findById(Long idEmpleado);
}
