package com.aydsii.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Demo en vivo de Solicitar devolución (CU-13) y Actualizar devolución (CU-22).
 * <p>
 * Solo se activa con el perfil {@code demo}:
 * <pre>
 *   ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
 *   (Windows PowerShell)  .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
 * </pre>
 * Cuando la aplicación termina de arrancar, llama por HTTP a los mismos endpoints que se prueban en
 * Swagger, uno por cada flujo (normal, alternativos y de excepción), y muestra el código de
 * respuesta esperado, el obtenido y el JSON. Entre medio se ven los logs del servicio (INFO, WARN,
 * DEBUG) y las notificaciones. Después la aplicación sigue levantada para seguir probando en Swagger.
 * Al reiniciar, {@code schema.sql} y {@code data.sql} dejan los datos como al principio.
 */
@Component
@Profile("demo")
public class DemoDevoluciones {

    private static final Logger log = LoggerFactory.getLogger(DemoDevoluciones.class);
    /** Solo para mostrar el JSON indentado; los montos se leen como BigDecimal para no perder decimales. */
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private final Environment environment;
    private RestClient cliente;

    public DemoDevoluciones(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ejecutar() {
        cliente = RestClient.create("http://localhost:" + environment.getProperty("local.server.port", "8080"));

        titulo("CU-13 SOLICITAR DEVOLUCIÓN DE PRODUCTO (actor: Cliente)");

        escenario("1. Flujo principal: Ana pide devolver la maceta del pedido 7 (entregado hace 3 días)", 201,
                HttpMethod.POST, "/api/devoluciones/pedidos/7",
                "{\"idProducto\": 1, \"motivo\": \"Llegó con una rajadura\", \"fotoProducto\": \"fotos/maceta-pedido-7.jpg\"}");

        log.info("---- 2. FA-01: el cliente no confirma la solicitud");
        log.info("     El front no invoca el servicio, así que no se registra nada. No hay llamada que mostrar.");

        escenario("3. FE-01: Carla ya tiene 2 devoluciones en los últimos 6 meses", 409,
                HttpMethod.POST, "/api/devoluciones/pedidos/8",
                "{\"idProducto\": 3, \"motivo\": null, \"fotoProducto\": \"fotos/botella-pedido-8.jpg\"}");

        escenario("4. RN-04: Ana repite la solicitud de la maceta (ya tiene una pendiente)", 409,
                HttpMethod.POST, "/api/devoluciones/pedidos/7",
                "{\"idProducto\": 1, \"motivo\": \"Otra vez\", \"fotoProducto\": \"fotos/maceta-pedido-7.jpg\"}");

        escenario("5. RN-02: el pedido 5 se entregó hace más de 10 días", 409,
                HttpMethod.POST, "/api/devoluciones/pedidos/5",
                "{\"idProducto\": 3, \"fotoProducto\": \"fotos/botella.jpg\"}");

        escenario("6. RN-01: el pedido 1 todavía no se entregó", 409,
                HttpMethod.POST, "/api/devoluciones/pedidos/1",
                "{\"idProducto\": 1, \"fotoProducto\": \"fotos/maceta.jpg\"}");

        escenario("7. El producto 5 no forma parte del pedido 7", 404,
                HttpMethod.POST, "/api/devoluciones/pedidos/7",
                "{\"idProducto\": 5, \"fotoProducto\": \"fotos/banco.jpg\"}");

        escenario("8. Falta la foto del producto", 400,
                HttpMethod.POST, "/api/devoluciones/pedidos/7",
                "{\"idProducto\": 2, \"motivo\": \"No me gustó\"}");

        titulo("CU-22 ACTUALIZAR DEVOLUCIÓN (actor: Ventas, empleado 7)");

        escenario("9. Servicio 1: Ventas ingresa el número de devolución 3 y ve sus datos", 200,
                HttpMethod.GET, "/api/devoluciones/3", null);

        escenario("10. Servicio 2: Ventas indica BUEN_ESTADO y confirma -> RESUELTA con el 100 %", 200,
                HttpMethod.PATCH, "/api/devoluciones/3/verificacion?idEmpleado=7", "{\"resultado\": \"BUEN_ESTADO\"}");

        escenario("11. FE-01: la devolución 3 ya está resuelta y no admite actualización", 409,
                HttpMethod.GET, "/api/devoluciones/3", null);

        escenario("12. FA-01 (servicio 1): Ventas consulta la devolución 4", 200,
                HttpMethod.GET, "/api/devoluciones/4", null);
        escenario("12. FA-01 (servicio 2): el producto llegó DANADO -> EN_VERIFICACION", 200,
                HttpMethod.PATCH, "/api/devoluciones/4/verificacion?idEmpleado=7", "{\"resultado\": \"DANADO\"}");

        escenario("13. FA-02: Ventas consulta la devolución 6 y cancela (no llama al servicio 2)", 200,
                HttpMethod.GET, "/api/devoluciones/6", null);
        escenario("13. FA-02: al volver a consultarla sigue EN_TRANSITO_A_DEVOLUCION, sin recepción registrada", 200,
                HttpMethod.GET, "/api/devoluciones/6", null);

        escenario("14. FE-01: la devolución 5 está PENDIENTE_DE_REVISION", 409,
                HttpMethod.GET, "/api/devoluciones/5", null);

        escenario("15. FE-01: el número de devolución 999 no existe", 404,
                HttpMethod.GET, "/api/devoluciones/999", null);

        escenario("16. El empleado 8 es transportista, no de Ventas", 403,
                HttpMethod.PATCH, "/api/devoluciones/6/verificacion?idEmpleado=8", "{\"resultado\": \"BUEN_ESTADO\"}");

        escenario("17. Resultado inexistente", 400,
                HttpMethod.PATCH, "/api/devoluciones/6/verificacion?idEmpleado=7", "{\"resultado\": \"ROTO\"}");

        titulo("FIN DE LA DEMO. Swagger: http://localhost:" + environment.getProperty("local.server.port", "8080")
                + "/swagger-ui.html  |  Notificaciones: logs/notificaciones.log");
    }

    /** Llama a un endpoint y muestra el código esperado, el obtenido y el JSON de la respuesta. */
    private void escenario(String descripcion, int esperado, HttpMethod metodo, String uri, String cuerpo) {
        log.info("---- {}", descripcion);
        log.info("     {} {}{}", metodo, uri, cuerpo == null ? "" : "   " + cuerpo);

        RestClient.RequestBodySpec pedido = cliente.method(metodo).uri(uri);
        if (cuerpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
        }
        ResponseEntity<String> respuesta = pedido.retrieve()
                .onStatus(estado -> true, (request, response) -> { })   // los 4xx no cortan la demo
                .toEntity(String.class);

        int obtenido = respuesta.getStatusCode().value();
        log.info("     Esperado {} | obtenido {} {}", esperado, obtenido, obtenido == esperado ? "[OK]" : "[DISTINTO]");
        log.info("     Respuesta:\n{}", lindo(respuesta.getBody()));
    }

    private static void titulo(String texto) {
        log.info("");
        log.info("==================== {} ====================", texto);
    }

    private static String lindo(String json) {
        if (json == null || json.isBlank()) {
            return "(sin cuerpo)";
        }
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(JSON.readTree(json));
    }
}
