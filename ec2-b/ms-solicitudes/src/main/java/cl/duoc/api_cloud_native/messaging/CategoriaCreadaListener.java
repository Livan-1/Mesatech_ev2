package cl.duoc.api_cloud_native.messaging;

import cl.duoc.api_cloud_native.entity.CategoriaLocal;
import cl.duoc.api_cloud_native.repository.CategoriaLocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumidor: guarda la copia en RDS Solicitudes y recien despues retorna.
 * Con acknowledge-mode=auto, Spring envia el ack solo si este metodo termina sin excepcion.
 * Si save() falla, la excepcion se propaga y el mensaje vuelve a la cola.
 */
@Component
public class CategoriaCreadaListener {

    private static final Logger log = LoggerFactory.getLogger(CategoriaCreadaListener.class);

    private final CategoriaLocalRepository repository;

    public CategoriaCreadaListener(CategoriaLocalRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "${mesatech.rabbit.queue}")
    public void recibir(CategoriaCreadaEvent evento) {
        log.info("[CONSUMIDOR] Mensaje recibido eventId={} categoriaId={} nombre={}",
                evento.eventId(), evento.categoriaId(), evento.nombre());

        if (repository.existsByCategoriaId(evento.categoriaId())) {
            log.warn("[CONSUMIDOR] categoriaId={} ya existe en rds-solicitudes; se ignora eventId={}",
                    evento.categoriaId(), evento.eventId());
            return;
        }

        CategoriaLocal copia = new CategoriaLocal();
        copia.setCategoriaId(evento.categoriaId());
        copia.setNombre(evento.nombre());
        copia.setEventId(evento.eventId());
        copia.setFechaEvento(Instant.parse(evento.fechaEvento()));
        CategoriaLocal guardada = repository.save(copia);

        log.info("[RDS SOLICITUDES] Copia guardada id={} categoriaId={} eventId={}",
                guardada.getId(), guardada.getCategoriaId(), guardada.getEventId());
    }
}
