package cl.duoc.api_cloud_native.messaging;

/** Mismo contrato que publica ms-catalogo. */
public record CategoriaCreadaEvent(
        String eventId,
        String tipoEvento,
        String fechaEvento,
        Long categoriaId,
        String nombre) {
}
