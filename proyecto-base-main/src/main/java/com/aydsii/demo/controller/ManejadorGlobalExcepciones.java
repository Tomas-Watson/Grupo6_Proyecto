package com.aydsii.demo.controller;

import com.aydsii.demo.dto.ErrorDTO;
import com.aydsii.demo.service.AccesoDenegadoException;
import com.aydsii.demo.service.CarritoVacioException;
import com.aydsii.demo.service.DevolucionNoPermitidaException;
import com.aydsii.demo.service.RecursoNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Traduce las excepciones a respuestas {@link ErrorDTO} con el código HTTP que corresponde.
 * <p>
 * Extiende {@link ResponseEntityExceptionHandler} para que los errores estándar de Spring MVC
 * (método no permitido, ruta inexistente, etc.) también respondan con {@link ErrorDTO}.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    static final String TITULO_CARRITO_VACIO = "Carrito vacío";
    static final String TITULO_CONFLICTO_STOCK = "Conflicto al reservar stock";
    static final String CONFLICTO_STOCK = "Otro cliente modificó el stock de uno o más productos mientras se "
            + "confirmaba la compra. No se registró el pedido ni se reservó stock y el carrito sigue igual: "
            + "vuelve a consultar el resumen y confirma de nuevo.";
    static final String TITULO_SOLICITUD_INVALIDA = "Solicitud inválida";
    static final String TITULO_ACCESO_DENEGADO = "Acceso denegado";
    static final String TITULO_ERROR_INTERNO = "Error interno";
    static final String ERROR_INTERNO = "Ocurrió un error inesperado. Si se estaba confirmando una compra, "
            + "no se registró el pedido ni se reservó stock.";

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    /** El cliente no tiene productos para comprar, o ninguno sigue publicado (FE-02): 409. */
    @ExceptionHandler(CarritoVacioException.class)
    public ResponseEntity<ErrorDTO> manejarCarritoVacio(CarritoVacioException excepcion, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, TITULO_CARRITO_VACIO, excepcion.getMessage(),
                request.getRequestURI(), excepcion.getProductosNoDisponibles());
    }

    /** El pedido, el producto del pedido, la solicitud de devolución o el empleado no existen: 404. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorDTO> manejarNoEncontrado(RecursoNoEncontradoException excepcion,
                                                        HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, excepcion.getTitulo(), excepcion.getMessage(),
                request.getRequestURI(), List.of());
    }

    /**
     * Devoluciones (CU-13 y CU-22): una regla de negocio no permite pedir o actualizar la devolución
     * (por ejemplo, el FE-01 de cada caso de uso): 409. No se registró nada.
     */
    @ExceptionHandler(DevolucionNoPermitidaException.class)
    public ResponseEntity<ErrorDTO> manejarDevolucionNoPermitida(DevolucionNoPermitidaException excepcion,
                                                                 HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, excepcion.getTitulo(), excepcion.getMessage(),
                request.getRequestURI(), List.of());
    }

    /** Consultar estado del pedido: el pedido es de otro cliente (RN-01): 403. */
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorDTO> manejarAccesoDenegado(AccesoDenegadoException excepcion,
                                                          HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, TITULO_ACCESO_DENEGADO, excepcion.getMessage(),
                request.getRequestURI(), List.of());
    }

    /**
     * Otro cliente modificó el stock mientras se confirmaba la compra (bloqueo optimista de
     * {@code Producto}): 409. La transacción ya se revirtió, así que no quedó nada a medias.
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorDTO> manejarConflictoDeStock(ConcurrencyFailureException excepcion,
                                                            HttpServletRequest request) {
        log.warn("Conflicto de concurrencia al reservar stock: {}", excepcion.getMessage());
        return responder(HttpStatus.CONFLICT, TITULO_CONFLICTO_STOCK, CONFLICTO_STOCK,
                request.getRequestURI(), List.of());
    }

    /** Cualquier otro error: 500, sin exponer detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> manejarErrorInesperado(Exception excepcion, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), excepcion);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, TITULO_ERROR_INTERNO, ERROR_INTERNO,
                request.getRequestURI(), List.of());
    }

    /**
     * Algún id de la ruta no cumple {@code @Positive} (cero o negativo), o algún campo del cuerpo
     * (por ejemplo, la foto en CU-13) no cumple su validación: 400. Los campos inválidos van en
     * {@code detalles}.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException excepcion,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<String> parametros = new ArrayList<>();
        List<String> camposInvalidos = new ArrayList<>();
        for (ParameterValidationResult resultado : excepcion.getParameterValidationResults()) {
            if (resultado instanceof ParameterErrors errores) {                    // cuerpo con @Valid
                errores.getFieldErrors().forEach(campo ->
                        camposInvalidos.add(campo.getField() + ": " + campo.getDefaultMessage()));
            } else if (resultado.getMethodParameter().getParameterName() != null) {
                parametros.add("'" + resultado.getMethodParameter().getParameterName() + "'");
            }
        }
        String mensaje;
        if (!camposInvalidos.isEmpty()) {
            mensaje = "Hay campos inválidos en el cuerpo de la solicitud.";
        } else if (parametros.isEmpty()) {
            mensaje = "Los identificadores deben ser números enteros positivos.";
        } else {
            mensaje = "El parámetro " + String.join(", ", parametros) + " debe ser un número entero positivo.";
        }
        ErrorDTO error = ErrorDTO.de(HttpStatus.BAD_REQUEST.value(), TITULO_SOLICITUD_INVALIDA,
                mensaje, ruta(request), camposInvalidos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(error);
    }

    /** Falta un parámetro obligatorio de la URL (por ejemplo, idEmpleado en CU-22): 400. */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException excepcion,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        ErrorDTO error = ErrorDTO.de(HttpStatus.BAD_REQUEST.value(), TITULO_SOLICITUD_INVALIDA,
                "Falta el parámetro obligatorio '" + excepcion.getParameterName() + "'.", ruta(request), List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(error);
    }

    /** El cuerpo no es un JSON válido o un valor no corresponde (por ejemplo, un resultado inexistente): 400. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException excepcion,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ErrorDTO error = ErrorDTO.de(HttpStatus.BAD_REQUEST.value(), TITULO_SOLICITUD_INVALIDA,
                "El cuerpo de la solicitud no tiene el formato esperado. Revisá el JSON (en CU-22, 'resultado' "
                        + "debe ser BUEN_ESTADO o DANADO).", ruta(request), List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(error);
    }

    /** Algún id de la ruta no es numérico: 400. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException excepcion, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String parametro = excepcion instanceof MethodArgumentTypeMismatchException conNombre
                ? conNombre.getName()
                : excepcion.getPropertyName();
        String mensaje = "El parámetro '" + parametro + "' debe ser un número entero positivo (valor recibido: '"
                + excepcion.getValue() + "').";
        ErrorDTO error = ErrorDTO.de(HttpStatus.BAD_REQUEST.value(), TITULO_SOLICITUD_INVALIDA, mensaje,
                ruta(request), List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(error);
    }

    /** Resto de los errores estándar de Spring MVC (404, 405, ...): mismo formato {@link ErrorDTO}. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception excepcion, Object cuerpo, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        String mensaje = switch (status.value()) {
            case 404 -> "No existe el recurso solicitado.";
            case 405 -> "El método HTTP no está permitido para esta ruta.";
            case 406 -> "No se puede responder en el formato solicitado. Usa application/json.";
            default -> cuerpo instanceof ProblemDetail detalle && detalle.getDetail() != null
                    ? detalle.getDetail()
                    : ERROR_INTERNO;
        };
        String titulo = status.is4xxClientError() ? TITULO_SOLICITUD_INVALIDA : TITULO_ERROR_INTERNO;
        ErrorDTO error = ErrorDTO.de(status.value(), titulo, mensaje, ruta(request), List.of());
        return ResponseEntity.status(status).headers(headers).body(error);
    }

    private static ResponseEntity<ErrorDTO> responder(HttpStatus status, String titulo, String mensaje, String ruta,
                                                      List<String> detalles) {
        return ResponseEntity.status(status).body(ErrorDTO.de(status.value(), titulo, mensaje, ruta, detalles));
    }

    private static String ruta(WebRequest request) {
        return request instanceof ServletWebRequest servlet
                ? servlet.getRequest().getRequestURI()
                : request.getDescription(false);
    }
}
