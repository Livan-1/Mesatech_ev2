package cl.duoc.api_cloud_native.messaging;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serializa los eventos como JSON. Spring Boot lo asigna automaticamente al RabbitTemplate.
 * No se declaran exchange, cola ni binding: ya existen en el broker y el usuario
 * pub_catalogo no tiene permiso "configure".
 */
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
