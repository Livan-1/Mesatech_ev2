package cl.duoc.api_cloud_native.messaging;

/**
 * Contrato del evento publicado al crear una categoria.
 * fechaEvento: ISO-8601 en UTC (ej. 2026-11-10T14:32:05.123Z).
 */
public record CategoriaCreadaEvent(
        String eventId,
        String tipoEvento,
        String fechaEvento,
        Long categoriaId,
        String nombre) {
}
