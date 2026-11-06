package cl.duoc.api_cloud_native.controller;

import cl.duoc.api_cloud_native.entity.Solicitud;
import cl.duoc.api_cloud_native.repository.CategoriaLocalRepository;
import cl.duoc.api_cloud_native.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/solicitudes")
public class SolicitudController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private CategoriaLocalRepository categoriaLocalRepository;

    // 1. Crear solicitud (POST /v1/solicitudes)
    @PostMapping
    public ResponseEntity<?> crearSolicitud(@RequestBody Solicitud solicitud) {
        // EP2: solo se aceptan categorias que ya llegaron por RabbitMQ a la copia local
        if (solicitud.getCategoria() == null || !categoriaLocalRepository.existsByNombre(solicitud.getCategoria())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "La categoría aún no está disponible en Solicitudes");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        Solicitud nueva = solicitudRepository.save(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    // 2. Listar todas las solicitudes (GET /v1/solicitudes)
    @GetMapping
    public List<Solicitud> listarTodas() {
        return solicitudRepository.findAll();
    }

    // 3. Listar solicitudes del usuario autenticado (GET /v1/solicitudes/mias?usuario=correo)
    @GetMapping("/mias")
    public List<Solicitud> listarPorUsuario(@RequestParam String usuario) {
        return solicitudRepository.findByUsuarioSolicitante(usuario);
    }

    // 4. Actualizar estado respetando la regla de negocio (PUT /v1/solicitudes/{id}/estado)
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<Solicitud> optSolicitud = solicitudRepository.findById(id);

        if (optSolicitud.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitud no encontrada");
        }

        Solicitud solicitud = optSolicitud.get();
        String nuevoEstado = body.get("estado");

        // Regla de negocio: no puede pasar a RESUELTA si antes no está EN_PROCESO
        if ("RESUELTA".equalsIgnoreCase(nuevoEstado) && !"EN_PROCESO".equalsIgnoreCase(solicitud.getEstado())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Regla de negocio violada: Para marcar como RESUELTA, debe estar previamente EN_PROCESO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        solicitud.setEstado(nuevoEstado.toUpperCase());
        solicitudRepository.save(solicitud);

        return ResponseEntity.ok(solicitud);
    }
}