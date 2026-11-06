package cl.duoc.api_cloud_native.messaging;

import cl.duoc.api_cloud_native.exception.PublicacionEventoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class CategoriaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CategoriaEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public CategoriaEventPublisher(RabbitTemplate rabbitTemplate,
                                   @Value("${mesatech.rabbit.exchange}") String exchange,
                                   @Value("${mesatech.rabbit.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        // Si el exchange no puede enrutar el mensaje a ninguna cola, el broker lo devuelve
        this.rabbitTemplate.setReturnsCallback(r -> log.error(
                "[PUBLICADOR] Mensaje devuelto sin ruta: replyCode={} replyText={} routingKey={}",
                r.getReplyCode(), r.getReplyText(), r.getRoutingKey()));
    }

    /**
     * Publica el evento como mensaje persistente y espera la confirmacion del broker.
     * @return eventId publicado
     */
    public String publicarCategoriaCreada(Long categoriaId, String nombre) {
        String eventId = UUID.randomUUID().toString();
        CategoriaCreadaEvent evento = new CategoriaCreadaEvent(
                eventId, "CATEGORIA_CREADA", Instant.now().toString(), categoriaId, nombre);
        CorrelationData correlation = new CorrelationData(eventId);

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento, message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setMessageId(eventId);
                return message;
            }, correlation);

            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new PublicacionEventoException(categoriaId, "NACK del broker: " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new PublicacionEventoException(categoriaId, "El mensaje no tuvo ruta hacia la cola");
            }
            log.info("[PUBLICADOR] Evento confirmado por RabbitMQ eventId={} categoriaId={} exchange={} routingKey={}",
                    eventId, categoriaId, exchange, routingKey);
            return eventId;

        } catch (PublicacionEventoException e) {
            log.error("[PUBLICADOR] Publicacion fallida eventId={} categoriaId={} causa={}",
                    eventId, categoriaId, e.getMessage());
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicacionEventoException(categoriaId, "Interrumpido esperando la confirmacion");
        } catch (Exception e) {
            log.error("[PUBLICADOR] No se pudo publicar eventId={} categoriaId={} causa={}",
                    eventId, categoriaId, e.toString());
            throw new PublicacionEventoException(categoriaId, "RabbitMQ no disponible o sin confirmacion");
        }
    }
}
