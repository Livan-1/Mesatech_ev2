package cl.duoc.api_cloud_native.exception;

public class PublicacionEventoException extends RuntimeException {

    private final Long categoriaId;

    public PublicacionEventoException(Long categoriaId, String detalle) {
        super(detalle);
        this.categoriaId = categoriaId;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }
}
