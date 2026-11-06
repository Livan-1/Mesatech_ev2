package cl.duoc.api_cloud_native.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuesta controlada (P7): la categoria quedo guardada en RDS Catalogo,
 * pero el evento no fue confirmado por RabbitMQ. No se revierte nada.
 */
@RestControllerAdvice
public class PublicacionErrorHandler {

    @ExceptionHandler(PublicacionEventoException.class)
    public ResponseEntity<Map<String, Object>> manejar(PublicacionEventoException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "EVENTO_NO_PUBLICADO");
        body.put("categoriaId", e.getCategoriaId());
        body.put("guardadaEnRdsCatalogo", true);
        body.put("mensaje", "La categoría se guardó en Catálogo, pero el evento no pudo publicarse. "
                + "Puede no estar disponible para crear solicitudes.");
        body.put("detalle", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
