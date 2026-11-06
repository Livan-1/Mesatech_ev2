package cl.duoc.api_cloud_native.controller;

import cl.duoc.api_cloud_native.Repository.ItemCatalogoRepository;
import cl.duoc.api_cloud_native.entity.ItemCatalogo;
import cl.duoc.api_cloud_native.messaging.CategoriaEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/catalogo")
public class CatalogoController {

    private static final Logger log = LoggerFactory.getLogger(CatalogoController.class);

    private final ItemCatalogoRepository catalogoRepository;
    private final CategoriaEventPublisher publisher;

    public CatalogoController(ItemCatalogoRepository catalogoRepository, CategoriaEventPublisher publisher) {
        this.catalogoRepository = catalogoRepository;
        this.publisher = publisher;
    }

    @GetMapping
    public List<ItemCatalogo> listarTodo() {
        return catalogoRepository.findAll();
    }

    @GetMapping("/tipo/{tipo}")
    public List<ItemCatalogo> listarPorTipo(@PathVariable String tipo) {
        return catalogoRepository.findByTipo(tipo.toUpperCase());
    }

    /**
     * EP2: guarda en RDS Catalogo y, si es una CATEGORIA, publica el evento.
     * Sin @Transactional a proposito: guardar y publicar son operaciones separadas (ver P7).
     */
    @PostMapping
    public ResponseEntity<?> agregarItem(@RequestBody ItemCatalogo item) {
        if (item.getNombre() == null || item.getNombre().isBlank() || item.getTipo() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "nombre y tipo son obligatorios"));
        }
        item.setNombre(item.getNombre().trim());
        item.setTipo(item.getTipo().toUpperCase());

        ItemCatalogo guardado = catalogoRepository.save(item);
        log.info("[RDS CATALOGO] Item guardado id={} nombre={} tipo={}",
                guardado.getId(), guardado.getNombre(), guardado.getTipo());

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("id", guardado.getId());
        respuesta.put("nombre", guardado.getNombre());
        respuesta.put("tipo", guardado.getTipo());

        if ("CATEGORIA".equals(guardado.getTipo())) {
            // Si falla, PublicacionErrorHandler responde 503 indicando que el registro quedo guardado
            String eventId = publisher.publicarCategoriaCreada(guardado.getId(), guardado.getNombre());
            respuesta.put("eventId", eventId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // Version 2 (EP1): catalogo extendido con metadatos
    @GetMapping("/v2/catalogo")
    public ResponseEntity<?> obtenerCatalogoV2() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "v2.0");
        response.put("fechaConsulta", java.time.LocalDateTime.now().toString());
        response.put("items", catalogoRepository.findAll());
        return ResponseEntity.ok(response);
    }
}
