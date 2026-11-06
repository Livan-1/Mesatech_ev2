package cl.duoc.api_cloud_native.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Copia local de una categoria recibida por RabbitMQ.
 * categoriaId conserva el id original de RDS Catalogo.
 */
@Entity
@Table(name = "categoria_local")
public class CategoriaLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "categoria_id", nullable = false, unique = true)
    private Long categoriaId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "fecha_evento", nullable = false)
    private Instant fechaEvento;

    @Column(name = "fecha_recepcion", nullable = false)
    private Instant fechaRecepcion;

    @PrePersist
    protected void onCreate() {
        this.fechaRecepcion = Instant.now();
    }

    public CategoriaLocal() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Instant getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(Instant fechaEvento) { this.fechaEvento = fechaEvento; }

    public Instant getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(Instant fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
}
