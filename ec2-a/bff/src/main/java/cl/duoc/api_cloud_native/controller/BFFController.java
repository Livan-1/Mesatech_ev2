package cl.duoc.api_cloud_native.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BFFController {

    @Autowired
    private RestTemplate restTemplate;

    // URLs internas hacia los microservicios de dominio
    @org.springframework.beans.factory.annotation.Value("${ORDERS_SERVICE_URL:http://localhost:8081/v1/solicitudes}")
    private String ORDERS_URL;

    @org.springframework.beans.factory.annotation.Value("${CATALOG_SERVICE_URL:http://localhost:8082/v1/catalogo}")
    private String CATALOG_URL;

    // EP2: categorias recibidas por RabbitMQ y guardadas en RDS Solicitudes
    @org.springframework.beans.factory.annotation.Value("${CATEGORIAS_LOCALES_URL:http://localhost:8081/v1/categorias-locales}")
    private String CATEGORIAS_LOCALES_URL;

    // 1. Endpoint que ya consumía tu React para comprobar sesión
    @GetMapping("/usuario")
    public ResponseEntity<?> obtenerUsuario(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> data = new HashMap<>();
        data.put("sub", jwt.getSubject());
        data.put("claims", jwt.getClaims());
        data.put("mensaje", "Autenticado correctamente en BFF");
        return ResponseEntity.ok(data);
    }

    // 2. Orquestación Catálogo: Obtener catálogo completo
    @GetMapping("/catalogo")
    public ResponseEntity<?> obtenerCatalogo() {
        try {
            Object response = restTemplate.getForObject(CATALOG_URL, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    // 3. Orquestación Catálogo: Agregar ítem (Solo Administrador)
    @PostMapping("/catalogo")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<?> agregarItemCatalogo(@RequestBody Object item) {
        try {
            Object response = restTemplate.postForObject(CATALOG_URL, item, Object.class);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    // 4. Orquestación Solicitudes: Crear nueva solicitud inyectando el correo del solicitante
    @PostMapping("/solicitudes")
    public ResponseEntity<?> crearSolicitud(@RequestBody Map<String, Object> body, @AuthenticationPrincipal Jwt jwt) {
        try {
            // Inyectamos de forma segura la identidad extraída del token validado por Entra ID
            String correo = jwt.getClaimAsString("preferred_username");
            if (correo == null) {
                correo = jwt.getClaimAsString("unique_name");
            }
            body.put("usuarioSolicitante", correo);

            Object response = restTemplate.postForObject(ORDERS_URL, body, Object.class);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    // 5. Orquestación Solicitudes: Listar todas (Para Operador y Administrador)
    @GetMapping("/solicitudes")
    @PreAuthorize("hasAnyAuthority('ROLE_OPERADOR','ROLE_ADMINISTRADOR')")
    public ResponseEntity<?> listarTodasLasSolicitudes() {
        try {
            Object response = restTemplate.getForObject(ORDERS_URL, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    // 6. Orquestación Solicitudes: Listar solo las mías (Para Cliente)
    @GetMapping("/solicitudes/mias")
    public ResponseEntity<?> listarMisSolicitudes(@AuthenticationPrincipal Jwt jwt) {
        try {
            String correo = jwt.getClaimAsString("preferred_username");
            if (correo == null) {
                correo = jwt.getClaimAsString("unique_name");
            }
            String url = ORDERS_URL + "/mias?usuario=" + correo;
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    // 7. Orquestación Solicitudes: Cambiar estado (Regla de negocio se delega a Orders)
    @PutMapping("/solicitudes/{id}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_OPERADOR','ROLE_ADMINISTRADOR')")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String url = ORDERS_URL + "/" + id + "/estado";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.PUT, request, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    // 8. EP2: categorias disponibles para crear solicitudes (copia local de ms-solicitudes)
    @GetMapping("/categorias-disponibles")
    public ResponseEntity<?> categoriasDisponibles() {
        try {
            Object response = restTemplate.getForObject(CATEGORIAS_LOCALES_URL, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

        // 9. Version 2 del catalogo: agrega version y fecha de consulta (v1 se mantiene sin cambios)
    @GetMapping("/v2/catalogo")
    public ResponseEntity<?> obtenerCatalogoV2() {
        try {
            Object response = restTemplate.getForObject(CATALOG_URL + "/v2/catalogo", Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
